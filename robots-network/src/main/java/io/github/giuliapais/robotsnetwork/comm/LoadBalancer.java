package io.github.giuliapais.robotsnetwork.comm;

import io.github.giuliapais.commons.DistrictBalancer;
import io.github.giuliapais.utils.MessagePrinter;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.ws.rs.client.Client;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class LoadBalancer extends Thread {

    private final ActivePeers activePeers = ActivePeers.getInstance();
    private final DistrictBalancer districtBalancer;
    private final HashMap<Integer, ManagedChannel> channels;
    private final HashMap<Integer, DistrictBalancingGrpc.DistrictBalancingBlockingStub> stubs = new HashMap<>();
    private final LogicalClock logicalClock;

    private final LoadBalancingMonitor loadBalancingMonitor = LoadBalancingMonitor.getInstance();
    private final HashMap<Integer, Integer> acks = new HashMap<>();
    private final HashMap<Integer, Integer> districts;

    private final Client restClient;
    private final String uriApi;


    public LoadBalancer(int robotId, HashMap<Integer, ManagedChannel> channels, DistrictBalancer districtBalancer,
                        Client restClient, String uriApi) {
        this.logicalClock = LogicalClock.getInstance(robotId);
        this.districtBalancer = districtBalancer;
        this.channels = channels;
        this.districts = new HashMap<>();
        this.restClient = restClient;
        this.uriApi = uriApi;
    }

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
            LoadBalancingResponse response = null;
            while (retryCount < P2PServiceManager.MAX_RETRY) {
                try {
                    response = stub.loadBalancingInitiation(request);
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
                acks.put(this.peerId, response.getAllowed() ? 1 : 0);
                districts.put(response.getRobotId(), response.getDistrict());
            } else {
                acks.put(this.peerId, -1);
            }
        }
    }

    private class PeerTerminateMessenger implements Runnable {
        private final int peerId;
        private final LoadBalancingTerminationMessage message;
        private final HashMap<Integer, Integer[]> newPos;

        public PeerTerminateMessenger(int peerId, LoadBalancingTerminationMessage message,
                                      HashMap<Integer, Integer[]> newPos) {
            this.peerId = peerId;
            this.message = message;
            this.newPos = newPos;
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
                    if (response.hasX()) {
                        synchronized (newPos) {
                            newPos.put(response.getRobotId(), new Integer[]{response.getX(), response.getY()});
                        }
                    }
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
                acks.put(this.peerId, 1);
            } else {
                acks.put(this.peerId, -1);
            }
        }
    }

    private void initiate() throws InterruptedException {
        List<Peer> peers = activePeers.getPeers();
        LoadBalancingRequest request = LoadBalancingRequest.newBuilder()
                .setRobotId(logicalClock.getRobotId())
                .setTimestamp(logicalClock.incrementAndGet())
                .build();
        for (Peer peer : peers) {
            Thread thread = new Thread(new PeerInitMessenger(peer.getId(), request));
            thread.start();
            thread.join();
        }
        // Ensure the process has an up-to-date state of the grid
        districtBalancer.upsert(districts);
        if (!acks.isEmpty() && acks.values().stream().allMatch(i -> i == 1)) {
            // Initiate load balancing procedure
            loadBalancingMonitor.setState(LoadBalancingMonitor.LoadBalancingState.REBALANCING);
        } else {
            // Check for crash events
            P2PServiceManager.crashDetection(acks, this.logicalClock);
            // Abort load balancing by falling back to the steady state
            loadBalancingMonitor.setState(LoadBalancingMonitor.LoadBalancingState.STEADY);
            acks.clear();
            districts.clear();
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
            districts.clear();
            return;
        }
        long timestamp = logicalClock.incrementAndGet();
        LoadBalancingTerminationMessage message = LoadBalancingTerminationMessage.newBuilder()
                .setRobotId(logicalClock.getRobotId())
                .setTimestamp(timestamp)
                .addAllChanges(changes.entrySet().stream()
                        .map(e -> LoadBalancingTerminationMessage.Change.newBuilder()
                                .setNewDistrict(e.getValue())
                                .setRobotId(e.getKey())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        loadBalancingMonitor.setRequestTimestamp(timestamp);
        List<Peer> peers = activePeers.getPeers();
        HashMap<Integer, Integer[]> newPos = new HashMap<>();
        for (Peer peer : peers) {
            Thread thread = new Thread(new PeerTerminateMessenger(peer.getId(), message, newPos));
            thread.start();
            thread.join();
        }
        districtBalancer.changeDistrict(changes);

        // TODO: contact the server and send the list of changes - from the response extract the new
        // position on the grid

        loadBalancingMonitor.setState(LoadBalancingMonitor.LoadBalancingState.STEADY);
        acks.clear();
        districts.clear();
        MessagePrinter.printMessage(
                "Load balancing completed successfully",
                MessagePrinter.INFO_FORMAT, true);
        if (!acks.isEmpty() && acks.values().stream().anyMatch(i -> i == -1)) {
            // Check for crash events
            P2PServiceManager.crashDetection(acks, this.logicalClock);
        }
    }


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
