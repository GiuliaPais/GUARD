package io.github.giuliapais.robotsnetwork.comm.p2p;

import io.github.giuliapais.commons.DistrictBalancer;
import io.github.giuliapais.commons.models.MapPosition;
import io.github.giuliapais.robotsnetwork.comm.*;
import io.github.giuliapais.utils.MessagePrinter;
import io.grpc.*;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class P2PServiceManager {
    /* ATTRIBUTES --------------------------------------------------------------------------------------------------- */
    static final int MAX_RETRY = 3;
    static final int START_BACKOFF_TIME = 2000;
    private final int robotId;
    private final int port;
    private final String selfIpAddress;
    private final ActivePeers peers;
    private Server grpcServer;
    private final RobotRepairStatus repairStatus;
    private final DistrictBalancer districtBalancer;

    private final HashMap<Integer, ManagedChannel> channels = new HashMap<>();
    private final HashMap<Integer, RepairServiceGrpc.RepairServiceBlockingStub> repairStubs = new HashMap<>();
    private final RepairAcks repairAcks = new RepairAcks();

    private LogicalClock logicalClock;
    private final AbstractCrashEventHandler[] crashEventHandlers;
    private final LoadBalancer loadBalancer;

    // Listeners
    private final MutExListener mutExListener = new MutExListener();
    private final RemovalListener removalListener = new RemovalListener();
    private final AddListener addListener = new AddListener();

    /* NESTED CLASSES ----------------------------------------------------------------------------------------------- */
    static class RepairAcks {

        private final HashMap<Integer, Integer> acksReceived = new HashMap<>();
        private final ArrayList<Integer> neededAcks = new ArrayList<>();

        public RepairAcks() {
        }

        public synchronized void addRequired(int peerId) {
            neededAcks.add(peerId);
        }

        public synchronized void addRequired(List<Integer> peerIds) {
            neededAcks.addAll(peerIds);
        }

        public synchronized void removeRequired(int peerId) {
            acksReceived.remove(peerId);
            neededAcks.remove((Integer) peerId);
        }

        public synchronized void addAck(int peerId, int ack) {
            acksReceived.put(peerId, ack);
            MessagePrinter.printMessage(
                    "Acks " + acksReceived.size() + " / " + neededAcks.size() + " received",
                    MessagePrinter.ACCENT_FORMAT, true
            );
            if (acksReceived.keySet().containsAll(neededAcks)) {
                notifyAll();
            }
        }

        public synchronized void waitForAcks() throws InterruptedException {
            while (!acksReceived.keySet().containsAll(neededAcks)) {
                wait();
            }
        }

        public synchronized void clear() {
            acksReceived.clear();
            neededAcks.clear();
        }

        public synchronized HashMap<Integer, Integer> getAcksReceived() {
            return acksReceived;
        }
    }

    class AddListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt instanceof IndexedPropertyChangeEvent) {
                IndexedPropertyChangeEvent event = (IndexedPropertyChangeEvent) evt;
                if (event.getPropertyName().equals("peers")) {
                    Peer oldPeer = (Peer) event.getOldValue();
                    Peer newPeer = (Peer) event.getNewValue();
                    if (oldPeer == null & newPeer != null) {
                        ManagedChannel channel;
                        synchronized (channels) {
                            channel = channels.get(newPeer.getId());
                            if (channel == null) {
                                channel = ManagedChannelBuilder.forAddress(newPeer.getIpAddress(), newPeer.getPort())
                                        .usePlaintext()
                                        .build();
                                channels.put(newPeer.getId(), channel);
                            }
                        }
                    }
                }
            }
        }
    }

    class MutExListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt instanceof IndexedPropertyChangeEvent) {
                IndexedPropertyChangeEvent event = (IndexedPropertyChangeEvent) evt;
                if (event.getPropertyName().equals("peers")) {
                    Peer oldPeer = (Peer) event.getOldValue();
                    Peer newPeer = (Peer) event.getNewValue();
                    if (repairStatus.getStatus() == RobotRepairStatus.RepairStatus.NEEDS_REPAIR &&
                            (oldPeer == null & newPeer != null)) {
                        repairAcks.addRequired(newPeer.getId());
                        sendRepairRequest(newPeer, repairStatus.getRequestTimestamp()).start();
                    }
                }
            }
        }
    }

    class RemovalListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt instanceof IndexedPropertyChangeEvent) {
                IndexedPropertyChangeEvent event = (IndexedPropertyChangeEvent) evt;
                if (event.getPropertyName().equals("peers")) {
                    Peer oldPeer = (Peer) event.getOldValue();
                    Peer newPeer = (Peer) event.getNewValue();
                    if (oldPeer != null & newPeer == null) {
                        if (repairStatus.getStatus() == RobotRepairStatus.RepairStatus.NEEDS_REPAIR) {
                            repairAcks.removeRequired(oldPeer.getId());
                        }
                        synchronized (repairStubs) {
                            repairStubs.remove(oldPeer.getId());
                        }
                        synchronized (channels) {
                            ManagedChannel channel = channels.get(oldPeer.getId());
                            if (channel != null) {
                                channel.shutdown();
                                channels.remove(oldPeer.getId());
                            }
                        }
                        districtBalancer.removeRobot(oldPeer.getId());
                    }
                }
            }
        }
    }


    /* CONSTRUCTORS ------------------------------------------------------------------------------------------------- */
    public P2PServiceManager(int robotId, int port, String selfIpAddress,
                             DistrictBalancer districtBalancer) {
        this.robotId = robotId;
        this.port = port;
        this.selfIpAddress = selfIpAddress;
        this.districtBalancer = districtBalancer;
        this.peers = ActivePeers.getInstance();
        this.peers.addPropertyChangeListener(mutExListener);
        this.peers.addPropertyChangeListener(removalListener);
        this.peers.addPropertyChangeListener(addListener);
        this.repairStatus = new RobotRepairStatus();
        this.logicalClock = LogicalClock.getInstance(robotId);
        this.crashEventHandlers = new AbstractCrashEventHandler[2];
        this.crashEventHandlers[0] = new CrashEventHandlerIn(this.channels);
        this.crashEventHandlers[1] = new CrashEventHandlerOut(this.channels);
        this.crashEventHandlers[0].start();
        this.crashEventHandlers[1].start();
        this.loadBalancer = new LoadBalancer(this.robotId, this.channels, this.districtBalancer);
        this.loadBalancer.start();
        startGrpcServer();
    }

    /* METHODS ------------------------------------------------------------------------------------------------------ */
    /* Private --------- */
    private void startGrpcServer() {
        MessagePrinter.printMessage("Starting gRPC server...", MessagePrinter.INFO_FORMAT, true);
        grpcServer = ServerBuilder.forPort(this.port)
                .addService(new IntroductionImpl(this.robotId, this.districtBalancer))
                .addService(new RequestRepairImpl(this.repairStatus, this.robotId))
                .addService(new CrashRecoveryImpl(this.robotId))
                .addService(new DistrictBalancingImpl(this.robotId, this.districtBalancer))
                .addService(new GracefulExitImpl(this.robotId))
                .build();
        try {
            grpcServer.start();
            MessagePrinter.printMessage("gRPC server started", MessagePrinter.INFO_FORMAT, true);
        } catch (IOException e) {
            throw new RuntimeException("Could not start gRPC server", e);
        }
    }

    private Thread sendIntroductionMessage(Peer peer, HashMap<Integer, Integer> acks) {
        Thread messageSender = new Thread(() -> {
            ManagedChannel channel;
            synchronized (channels) {
                channel = channels.get(peer.getId());
                if (channel == null) {
                    channel = ManagedChannelBuilder.forAddress(peer.getIpAddress(), peer.getPort())
                            .usePlaintext()
                            .build();
                    channels.put(peer.getId(), channel);
                }
            }
            IntroduceMeGrpc.IntroduceMeBlockingStub blockingStub = IntroduceMeGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(10, TimeUnit.SECONDS);
            MapPosition myPosition = districtBalancer.getRobotPosition(this.robotId);
            IntroduceMeRequest request = IntroduceMeRequest.newBuilder()
                    .setRobotId(this.robotId)
                    .setIpAddress(this.selfIpAddress)
                    .setPort(this.port)
                    .setDistrict(myPosition.getDistrict())
                    .setX(myPosition.getX())
                    .setY(myPosition.getY())
                    .build();

            int retryCount = 0;
            long backoffTime = START_BACKOFF_TIME;
            boolean success = false;

            while (retryCount < MAX_RETRY) {
                try {
                    // Send the request and wait for the peer to respond to the introduction
                    IntroduceMeResponse response = blockingStub.introduceMe(request);
                    MapPosition peerPosition = new MapPosition(response.getDistrict(),
                            response.getX(), response.getY());
                    districtBalancer.addRobot(response.getRobotId(), peerPosition);
                    success = true;
                    break;
                } catch (StatusRuntimeException e) {
                    // When it's impossible to contact a peer try again with exponential backoff
                    Status.Code code = e.getStatus().getCode();
                    if (code == Status.Code.UNAVAILABLE || code == Status.Code.UNKNOWN ||
                            code == Status.Code.INTERNAL ||
                            code == Status.Code.DEADLINE_EXCEEDED || code == Status.Code.ABORTED ||
                            code == Status.Code.CANCELLED) {
                        try {
                            MessagePrinter.printMessage("Unable to contact peer " + peer.getId() +
                                    ", retrying in " +
                                    backoffTime + "ms (attempt " + (retryCount + 1) +
                                    "/" + MAX_RETRY + ")", MessagePrinter.WARNING_FORMAT, true);
                            Thread.sleep(backoffTime);
                        } catch (InterruptedException ex) {
                            throw new RuntimeException("Interrupted while introducing to peer " + peer.getId(), ex);
                        }
                        backoffTime *= 2;
                        retryCount++;
                    } else {
                        throw new RuntimeException("Error while introducing to peer " + peer.getId(), e);
                    }
                }
            }

            if (success) {
                synchronized (acks) {
                    acks.put(peer.getId(), 1);
                }
            } else {
                synchronized (acks) {
                    acks.put(peer.getId(), -1);
                }
            }
        });
        messageSender.start();
        return messageSender;
    }

    private Thread sendRepairRequest(Peer peer, long timestamp) {
        return new Thread(() -> {
            // Retrieve the channel for the peer, if it does not exist create it
            ManagedChannel channel;
            synchronized (channels) {
                channel = channels.get(peer.getId());
                if (channel == null) {
                    channel = ManagedChannelBuilder.forAddress(peer.getIpAddress(), peer.getPort())
                            .usePlaintext()
                            .build();
                    channels.put(peer.getId(), channel);
                }
            }
            // Retrieve the stub for the peer, if it does not exist create it
            RepairServiceGrpc.RepairServiceBlockingStub blockingStub;
            synchronized (repairStubs) {
                blockingStub = repairStubs.get(peer.getId());
                if (blockingStub == null) {
                    blockingStub = RepairServiceGrpc.newBlockingStub(channel);
                    repairStubs.put(peer.getId(), blockingStub);
                }
            }
            RepairRequest request = RepairRequest.newBuilder()
                    .setRobotId(this.robotId)
                    .setTimestamp(timestamp)
                    .build();

            int retryCount = 0;
            long backoffTime = START_BACKOFF_TIME;
            boolean success = false;

            while (retryCount < MAX_RETRY) {
                try {
                    RepairResponse response = blockingStub.requestRepair(request);
                    logicalClock.compareAndAdjust(response.getTimestamp());
                    success = true;
                    break;
                } catch (StatusRuntimeException e) {
                    Status.Code code = e.getStatus().getCode();
                    if (code == Status.Code.UNAVAILABLE || code == Status.Code.UNKNOWN ||
                            code == Status.Code.INTERNAL ||
                            code == Status.Code.DEADLINE_EXCEEDED || code == Status.Code.ABORTED ||
                            code == Status.Code.CANCELLED) {
                        try {
                            MessagePrinter.printMessage("Unable to contact peer " + peer.getId() +
                                    ", retrying in " +
                                    backoffTime + "ms (attempt " + (retryCount + 1) +
                                    "/" + MAX_RETRY + ")", MessagePrinter.WARNING_FORMAT, true);
                            Thread.sleep(backoffTime);
                        } catch (InterruptedException ex) {
                            throw new RuntimeException("Interrupted during mutual exclusion " + peer.getId(), ex);
                        }
                        backoffTime *= 2;
                        retryCount++;
                    } else {
                        throw new RuntimeException("Error while trying to contact peer " + peer.getId(), e);
                    }
                }
            }

            if (success) {
                this.repairAcks.addAck(peer.getId(), 1);
            } else {
                this.repairAcks.addAck(peer.getId(), -1);
            }
        });
    }

    private List<Thread> setUpAccessRequests(List<Peer> randomPeers, String type, HashMap<Integer, Integer> acks) {
        List<Thread> accessThreads = new ArrayList<>();
        long timestamp = logicalClock.incrementAndGet();
        for (Peer peer : randomPeers) {
            RepairServiceGrpc.RepairServiceBlockingStub stub;
            synchronized (repairStubs) {
                stub = repairStubs.get(peer.getId());
            }
            if (stub != null) {
                Thread accessThread = new Thread(() -> {
                    AccessRequest request = AccessRequest.newBuilder()
                            .setRobotId(this.robotId)
                            .setTimestamp(timestamp)
                            .setType(type)
                            .build();
                    try {
                        AccessResponse response = stub.accessCrashDetection(request);
                        logicalClock.compareAndAdjust(response.getTimestamp());
                        synchronized (acks) {
                            acks.put(peer.getId(), 1);
                        }
                    } catch (StatusRuntimeException e) {
                        Status.Code code = e.getStatus().getCode();
                        if (code == Status.Code.UNAVAILABLE || code == Status.Code.UNKNOWN ||
                                code == Status.Code.INTERNAL ||
                                code == Status.Code.DEADLINE_EXCEEDED || code == Status.Code.ABORTED ||
                                code == Status.Code.CANCELLED) {
                            synchronized (acks) {
                                acks.put(peer.getId(), -1);
                            }
                        } else {
                            throw new RuntimeException("Error while trying to contact peer " + peer.getId(), e);
                        }
                    }
                });
                accessThreads.add(accessThread);
            }
        }
        return accessThreads;
    }


    private void goToMechanic() throws InterruptedException {
        MessagePrinter.printMessage("Going to mechanic", MessagePrinter.INFO_FORMAT, true);
        List<Peer> activePeers = peers.getPeers();
        if (activePeers.isEmpty()) {
            // If the robot is alone, go directly to mechanic
            Thread.sleep(10000);
            repairStatus.repairFinished();
            repairAcks.clear();
            MessagePrinter.printMessage("Repairs done!", MessagePrinter.INFO_FORMAT, true);
            return;
        }
        // - Pick 3 peers at random from active peers
        HashMap<Integer, Integer> acks = new HashMap<>();
        int peersToPick;
        List<Peer> randomPeers;
        // Set up access messages
        List<Thread> accessMsgs;
        while (true) {
            peersToPick = Math.min(3, activePeers.size());
            Collections.shuffle(activePeers);
            randomPeers = activePeers.subList(0, peersToPick);
            if (randomPeers.isEmpty()) {
                activePeers = peers.getPeers();
                if (activePeers.isEmpty()) {
                    break;
                }
                continue;
            }
            accessMsgs = setUpAccessRequests(randomPeers, "access", acks);
            for (Thread thread : accessMsgs) {
                thread.start();
                thread.join();
            }
            crashDetection(acks, this.logicalClock);
            if (acks.values().stream().allMatch(i -> i == -1)) {
                // Can't communicate with any of the peers, try again
                activePeers = peers.getPeers();
                if (activePeers.isEmpty()) {
                    break;
                }
                acks.clear();
                continue;
            }
            break;
        }
        Thread.sleep(10000); // Mechanic repair time
        // Set up release messages
        if (!randomPeers.isEmpty()) {
            acks.clear();
            List<Thread> releaseMsgs = setUpAccessRequests(randomPeers, "release", acks);
            for (Thread thread : releaseMsgs) {
                thread.start();
                thread.join();
            }
        }
        crashDetection(acks, this.logicalClock);
        repairStatus.repairFinished();
        repairAcks.clear();
        MessagePrinter.printMessage("Repairs done!", MessagePrinter.INFO_FORMAT, true);
    }

    private Thread getGoodbyeSender(Peer peer, long timestamp, List<Integer> acks) {
        return new Thread(() -> {
            // Retrieve the channel for the peer, if it does not exist create it
            ManagedChannel channel;
            synchronized (channels) {
                channel = channels.get(peer.getId());
                if (channel == null) {
                    channel = ManagedChannelBuilder.forAddress(peer.getIpAddress(), peer.getPort())
                            .usePlaintext()
                            .build();
                    channels.put(peer.getId(), channel);
                }
            }
            // Create the stub
            GracefulExitGrpc.GracefulExitBlockingStub blockingStub = GracefulExitGrpc.newBlockingStub(channel);
            GoodbyeMessage message = GoodbyeMessage.newBuilder()
                    .setRobotId(this.robotId)
                    .setTimestamp(timestamp)
                    .build();
            try {
                GoodbyeAck response = blockingStub.sayGoodbye(message);
                logicalClock.compareAndAdjust(response.getTimestamp());
                synchronized (acks) {
                    acks.add(1);
                }
            } catch (StatusRuntimeException e) {
                Status.Code code = e.getStatus().getCode();
                if (code == Status.Code.UNAVAILABLE || code == Status.Code.UNKNOWN ||
                        code == Status.Code.INTERNAL ||
                        code == Status.Code.DEADLINE_EXCEEDED || code == Status.Code.ABORTED ||
                        code == Status.Code.CANCELLED) {
                    synchronized (acks) {
                        acks.add(-1);
                    }
                }
            }
        });
    }

    static void crashDetection(HashMap<Integer, Integer> acks, LogicalClock logicalClock) {
        // Check if any of the peers crashed
        if (!acks.isEmpty() && acks.values().stream().anyMatch(i -> i == -1)) {
            HashMap<Integer, Integer> crashedPeers = (HashMap<Integer, Integer>) acks.entrySet().stream()
                    .filter(entry -> entry.getValue() == -1)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            // Add crash events to the monitor
            CrashEventMonitor monitor = CrashEventMonitor.getInstance(CrashEventMonitor.CrashEventMonitorType.OUTGOING);
            for (Integer peerId : crashedPeers.keySet()) {
                CrashEvent evt = CrashEvent.newBuilder()
                        .setRobotId(logicalClock.getRobotId())
                        .setCrashedRobotId(peerId)
                        .setTimestamp(logicalClock.incrementAndGet())
                        .build();
                monitor.addCrashEvent(evt);
            }
        }
    }

    /* Public ---------- */
    public void introduceMe() {
        HashMap<Integer, Integer> acks = new HashMap<>();
        synchronized (peers) {
            if (peers.isEmpty()) {
                return;
            }
            MessagePrinter.printMessage("Sending introduction messages...",
                    MessagePrinter.INFO_FORMAT, true);
            for (Peer peer : peers.getPeers()) {
                Thread sender = sendIntroductionMessage(peer, acks);
                try {
                    sender.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while waiting for introduction acks", e);
                }
            }
            crashDetection(acks, this.logicalClock);
        }
    }

    public void requestRepair() {
        MessagePrinter.printMessage("I need repairs... Initiating protocol",
                MessagePrinter.ACCENT_FORMAT_2, true);
        List<Thread> senders = new ArrayList<>();
        long timestamp = this.logicalClock.incrementAndGet();
        repairStatus.needsRepairs(timestamp);
        if (!peers.isEmpty()) {
            // Executes only if the robot is not alone in the grid (otherwise no need for mutual exclusion)
            MessagePrinter.printMessage("Coordinating for repairs... Sending requests with timestamp "
                            + timestamp + ", waiting for acks",
                    MessagePrinter.INFO_FORMAT, true);
            this.repairAcks.addRequired(peers.getIds());
            for (Peer peer : peers.getPeers()) {
                Thread sender = sendRepairRequest(peer, timestamp);
                senders.add(sender);
            }
        }
        if (!senders.isEmpty()) {
            senders.forEach(Thread::start);
            senders.forEach(thread -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        try {
            this.repairAcks.waitForAcks();
            crashDetection(repairAcks.getAcksReceived(), this.logicalClock);
            repairStatus.setRepairing();
            goToMechanic();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for repair acks", e);
        }
    }

    public void gracefulStop() {
        long timestamp = this.logicalClock.incrementAndGet();
        List<Integer> acks = new ArrayList<>();
        if (!peers.isEmpty()) {
            MessagePrinter.printMessage("Sending goodbye messages...",
                    MessagePrinter.INFO_FORMAT, true);
            for (Peer peer : peers.getPeers()) {
                Thread sender = getGoodbyeSender(peer, timestamp, acks);
                sender.start();
                try {
                    sender.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        // Stop crash handlers
        for (AbstractCrashEventHandler handler : crashEventHandlers) {
            handler.interrupt();
        }
        // Stop the load balancer
        loadBalancer.interrupt();
        // Remove listeners
        peers.removePropertyChangeListener(mutExListener);
        peers.removePropertyChangeListener(removalListener);
        peers.removePropertyChangeListener(addListener);
        // Close channels
        synchronized (channels) {
            for (ManagedChannel channel : channels.values()) {
                channel.shutdown();
            }
        }
        // Stop grpc server
        grpcServer.shutdown();
    }

}
