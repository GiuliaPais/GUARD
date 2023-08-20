package io.github.giuliapais.robotsnetwork.comm.p2p;

import io.github.giuliapais.commons.DistrictBalancer;
import io.github.giuliapais.commons.models.MapPosition;
import io.github.giuliapais.robotsnetwork.comm.*;
import io.github.giuliapais.robotsnetwork.comm.rest.RestServiceManager;
import io.github.giuliapais.commons.MessagePrinter;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This thread is launched when the robot is initialised and monitors the need for load balancing until the process
 * is stopped.
 */
public class LoadBalancer extends Thread {
    /* ATTRIBUTES --------------------------------------------------------------------------------------------------- */
    private final ActivePeers activePeers = ActivePeers.getInstance();
    private final DistrictBalancer districtBalancer;
    private final HashMap<Integer, ManagedChannel> channels;
    private final HashMap<Integer, DistrictBalancingGrpc.DistrictBalancingBlockingStub> stubs = new HashMap<>();
    private final LogicalClock logicalClock;

    private final LoadBalancingMonitor loadBalancingMonitor = LoadBalancingMonitor.getInstance();
    private LoadBalancingAcks acks;
    private final HashMap<Integer, MapPosition> positions = new HashMap<>();

    /* NESTED CLASSES ----------------------------------------------------------------------------------------------- */

    private class LoadBalancingAcks {
        private final HashMap<Integer, Integer> acks = new HashMap<>();
        private final List<Integer> requiredAcks = new ArrayList<>();

        public LoadBalancingAcks(List<Integer> requiredAcks) {
            this.requiredAcks.addAll(requiredAcks);
        }

        public synchronized void addAck(int peerId, int ack) {
            acks.put(peerId, ack);
            if (acks.size() == requiredAcks.size()) {
                notify();
            }
        }

        public synchronized void waitAcks() {
            while (acks.size() < requiredAcks.size()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public synchronized void clear() {
            acks.clear();
            requiredAcks.clear();
        }

        public synchronized HashMap<Integer, Integer> getAcks() {
            return new HashMap<>(this.acks);
        }
    }

    /**
     * Dedicated thread for a single peer - sends the request to initiate the load balancing protocol and puts an
     * ack into the map.
     * <p>
     * Acks can be 1: allowed, 0: denied, -1: process crashed (unable to contact the peer)
     * Only if acks from all peers are 1, the process can start.
     */
    private class PeerInitMessenger implements Runnable {
        private final int peerId;
        private final LoadBalancingRequest request;

        public PeerInitMessenger(int peerId, LoadBalancingRequest request) {
            this.peerId = peerId;
            this.request = request;
        }

        @Override
        public void run() {
            // Exponential backoff strategy
            int retryCount = 0;
            long backoffTime = P2PServiceManager.START_BACKOFF_TIME;
            boolean success = false;
            // Get the stub
            DistrictBalancingGrpc.DistrictBalancingBlockingStub stub = stubs.get(this.peerId);
            if (stub == null) {
                if (channels.get(this.peerId) == null) {
                    return;
                }
                stub = DistrictBalancingGrpc.newBlockingStub(channels.get(this.peerId));
                stubs.put(this.peerId, stub);
            }
            LoadBalancingResponse response = null;
            while (retryCount < P2PServiceManager.MAX_RETRY) {
                try {
                    response = stub.loadBalancingInitiation(request);
                    MessagePrinter.printMessage("Load balancing initiation request sent to peer " +
                                    this.peerId + " with timestamp " + request.getTimestamp(),
                            MessagePrinter.ERROR_FORMAT, true);
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
                            MessagePrinter.printMessage("Unable to contact peer " + this.peerId +
                                            ", retrying in " +
                                            backoffTime + "ms (attempt " + (retryCount + 1) +
                                            "/" + P2PServiceManager.MAX_RETRY + ")",
                                    MessagePrinter.WARNING_FORMAT, true);
                            Thread.sleep(backoffTime);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        backoffTime *= 2;
                        retryCount++;
                    } else {
                        throw new RuntimeException("Error while trying to contact peer " + this.peerId, e);
                    }
                }
            }

            if (success) {
                acks.addAck(this.peerId, response.getAllowed() ? 1 : 0);
                positions.put(response.getRobotId(),
                        new MapPosition(response.getDistrict(), response.getX(), response.getY()));
            } else {
                acks.addAck(this.peerId, -1);
            }
        }
    }

    /**
     * Dedicated thread for a single peer - sends the request to terminate the load balancing protocol and puts an
     * ack into the map.
     * NOTE: the requests are sent to the peers only if there are changes to be made
     */
    private class PeerTerminateMessenger implements Runnable {
        private final int peerId;
        private final LoadBalancingTerminationMessage message;

        public PeerTerminateMessenger(int peerId, LoadBalancingTerminationMessage message) {
            this.peerId = peerId;
            this.message = message;
        }

        @Override
        public void run() {
            // Exponential backoff strategy
            int retryCount = 0;
            long backoffTime = 1000;
            boolean success = false;
            // Get the stub
            DistrictBalancingGrpc.DistrictBalancingBlockingStub stub = stubs.get(this.peerId);
            if (stub == null) {
                if (channels.get(this.peerId) == null) {
                    return;
                }
                stub = DistrictBalancingGrpc.newBlockingStub(channels.get(this.peerId));
                stubs.put(this.peerId, stub);
            }
            LoadBalancingTerminationAck response = null;
            while (retryCount < P2PServiceManager.MAX_RETRY) {
                try {
                    response = stub.loadBalancingTermination(message);
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
                            MessagePrinter.printMessage("Unable to contact peer " + this.peerId +
                                            ", retrying in " +
                                            backoffTime + "ms (attempt " + (retryCount + 1) +
                                            "/" + P2PServiceManager.MAX_RETRY + ")",
                                    MessagePrinter.WARNING_FORMAT, true);
                            Thread.sleep(backoffTime);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        backoffTime *= 2;
                        retryCount++;
                    } else {
                        throw new RuntimeException("Error while trying to contact peer " + this.peerId, e);
                    }
                }
            }

            if (success) {
                acks.addAck(this.peerId, 1);
            } else {
                acks.addAck(this.peerId, -1);
            }
        }
    }

    /* CONSTRUCTORS ------------------------------------------------------------------------------------------------- */
    public LoadBalancer(int robotId, HashMap<Integer, ManagedChannel> channels, DistrictBalancer districtBalancer) {
        this.logicalClock = LogicalClock.getInstance(robotId);
        this.districtBalancer = districtBalancer;
        this.channels = channels;
    }

    /* METHODS ------------------------------------------------------------------------------------------------------ */
    /* Private --------- */
    public void printGridStatus() {
        HashMap<Integer, List<Integer>> grid = districtBalancer.getSnapshot();
        MessagePrinter.printMessage("Grid status:", MessagePrinter.INFO_FORMAT, true);
        for (Map.Entry<Integer, List<Integer>> entry : grid.entrySet()) {
            MessagePrinter.printMessage("District " + entry.getKey() + " -> " + entry.getValue(),
                    MessagePrinter.INFO_FORMAT, true);
        }
    }

    private void initiate() throws InterruptedException {
        List<Peer> peers = activePeers.getPeers();
        MessagePrinter.printMessage(
                "Active peers: " + peers.stream().map(Peer::getId).toList(),
                MessagePrinter.ERROR_FORMAT, true
        );
        if (peers.isEmpty()) {
            MessagePrinter.printMessage(
                    "No need for load balancing at this moment",
                    MessagePrinter.INFO_FORMAT, true);
            loadBalancingMonitor.setState(LoadBalancingMonitor.LoadBalancingState.STEADY);
            if (acks != null) {
                acks.clear();
            }
            if (positions != null) {
                positions.clear();
            }
            return;
        }
        acks = new LoadBalancingAcks(peers.stream().map(Peer::getId).toList());
        long timestamp = logicalClock.incrementAndGet();
        LoadBalancingRequest request = LoadBalancingRequest.newBuilder()
                .setRobotId(logicalClock.getRobotId())
                .setTimestamp(timestamp)
                .build();
        loadBalancingMonitor.setRequestTimestamp(timestamp);
        for (Peer peer : peers) {
            Thread thread = new Thread(new PeerInitMessenger(peer.getId(), request));
            thread.start();
            thread.join();
        }
        acks.waitAcks();
        for (Map.Entry<Integer, Integer> peerAck : acks.getAcks().entrySet()) {
            MessagePrinter.printMessage(
                    "Peer " + peerAck.getKey() + " -> " + peerAck.getValue(),
                    MessagePrinter.ERROR_FORMAT, true
            );
        }
        // Ensure the process has an up-to-date state of the grid
        districtBalancer.upsert(positions);
        if (!acks.getAcks().isEmpty() && acks.getAcks().values().stream().allMatch(i -> i == 1)) {
            // Initiate load balancing procedure
            loadBalancingMonitor.setState(LoadBalancingMonitor.LoadBalancingState.REBALANCING);
        } else {
            // Check for crash events
            P2PServiceManager.crashDetection(acks.getAcks(), this.logicalClock);
            // Abort load balancing by falling back to the steady state
            MessagePrinter.printMessage(
                    "Detected crashes or another process is taking care of district load balancing",
                    MessagePrinter.INFO_FORMAT, true
            );
            loadBalancingMonitor.setState(LoadBalancingMonitor.LoadBalancingState.STEADY);
            acks.clear();
            positions.clear();
        }
    }

    private void checkBalance() throws InterruptedException {
        HashMap<Integer, Integer> changes = districtBalancer.rebalance();
        if (changes == null) {
            MessagePrinter.printMessage(
                    "No need for load balancing at this moment",
                    MessagePrinter.INFO_FORMAT, true);
            loadBalancingMonitor.setState(LoadBalancingMonitor.LoadBalancingState.STEADY);
            acks.clear();
            positions.clear();
            return;
        }
        // If there are changes get random positions for the robots
        HashMap<Integer, MapPosition> newPositions = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : changes.entrySet()) {
            int[] pos = districtBalancer.getPosInDistrict(entry.getValue());
            newPositions.put(entry.getKey(), new MapPosition(entry.getValue(), pos[0], pos[1]));
        }
        long timestamp = logicalClock.incrementAndGet();
        LoadBalancingTerminationMessage message = LoadBalancingTerminationMessage.newBuilder()
                .setRobotId(logicalClock.getRobotId())
                .setTimestamp(timestamp)
                .addAllChanges(newPositions.entrySet().stream()
                        .map(e -> LoadBalancingTerminationMessage.Change.newBuilder()
                                .setNewDistrict(e.getValue().getDistrict())
                                .setNewX(e.getValue().getX())
                                .setNewY(e.getValue().getY())
                                .setRobotId(e.getKey())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        loadBalancingMonitor.setRequestTimestamp(timestamp);
        List<Peer> peers = activePeers.getPeers();
        acks = new LoadBalancingAcks(peers.stream().map(Peer::getId).toList());
        // Send out termination messages and wait for acks
        for (Peer peer : peers) {
            Thread thread = new Thread(new PeerTerminateMessenger(peer.getId(), message));
            thread.start();
            thread.join();
        }
        acks.waitAcks();
        districtBalancer.updatePositions(newPositions); // Persist changes
        // Contact the server with the list of changes
        RestServiceManager.getInstance(null).updatePositions(newPositions);

        // If the position of the current robot has changed, signal it to ensure MQTT topic also changes
        if (changes.containsKey(logicalClock.getRobotId())) {
            ChangeDistrictMonitor changeDistrictMonitor = ChangeDistrictMonitor.getInstance();
            changeDistrictMonitor.districtChanged(changes.get(logicalClock.getRobotId()));
        }

        loadBalancingMonitor.setState(LoadBalancingMonitor.LoadBalancingState.STEADY);
        acks.clear();
        positions.clear();
        MessagePrinter.printMessage(
                "Load balancing completed successfully",
                MessagePrinter.INFO_FORMAT, true);
        printGridStatus();
        if (!acks.getAcks().isEmpty() && acks.getAcks().values().stream().anyMatch(i -> i == -1)) {
            // Check for crash events
            P2PServiceManager.crashDetection(acks.getAcks(), this.logicalClock);
        }
    }

    /* Public ---------- */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            LoadBalancingMonitor.LoadBalancingState state = loadBalancingMonitor.waitForNotSteady();
            if (state == LoadBalancingMonitor.LoadBalancingState.EVALUATING) {
                MessagePrinter.printMessage(
                        "Evaluating the need for load balancing on districts...",
                        MessagePrinter.INFO_FORMAT, true);
                try {
                    initiate();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                try {
                    MessagePrinter.printMessage(
                            "Checking...",
                            MessagePrinter.INFO_FORMAT, true);
                    checkBalance();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        }
    }
}
