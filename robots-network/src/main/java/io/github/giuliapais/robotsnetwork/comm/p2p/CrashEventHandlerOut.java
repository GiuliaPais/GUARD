package io.github.giuliapais.robotsnetwork.comm.p2p;

import io.github.giuliapais.robotsnetwork.comm.CrashEvent;
import io.github.giuliapais.robotsnetwork.comm.CrashEventResponse;
import io.github.giuliapais.robotsnetwork.comm.CrashRecoveryGrpc;
import io.github.giuliapais.robotsnetwork.comm.rest.RestServiceManager;
import io.github.giuliapais.commons.MessagePrinter;
import io.grpc.ManagedChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CrashEventHandlerOut extends AbstractCrashEventHandler {

    public CrashEventHandlerOut(HashMap<Integer, ManagedChannel> channels) {
        super(CrashEventMonitor.getInstance(CrashEventMonitor.CrashEventMonitorType.OUTGOING), channels);
    }

    static class CrashResponses {
        private final List<Boolean> responses = new ArrayList<>();
        private final int expectedResponses;

        public CrashResponses(int expectedResponses) {
            this.expectedResponses = expectedResponses;
        }

        public synchronized void addResponse(boolean response) {
            responses.add(response);
        }

        public synchronized void waitForResponses() throws InterruptedException {
            while (responses.size() < expectedResponses) {
                wait();
            }
        }

        public synchronized List<Boolean> getResponses() {
            return List.copyOf(responses);
        }
    }

    // Dedicated thread to notify the peer
    class PeerNotifier implements Runnable {
        private final int peerId;
        private final CrashEvent crashEvent;
        private final CrashResponses responses;

        public PeerNotifier(int peerId, CrashEvent crashEvent, CrashResponses responses) {
            this.peerId = peerId;
            this.crashEvent = crashEvent;
            this.responses = responses;
        }

        @Override
        public void run() {
            CrashRecoveryGrpc.CrashRecoveryBlockingStub stub = stubs.get(peerId);
            if (stub == null) {
                if (channels.get(peerId) == null) {
                    return;
                }
                stub = CrashRecoveryGrpc.newBlockingStub(channels.get(peerId));
                stubs.put(peerId, stub);
            }
            try {
                CrashEventResponse response = stub.notifyCrashEvent(crashEvent);
                synchronized (responses) {
                    responses.addResponse(response.getIsCrashed());
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                CrashEvent crashEvent = crashEventsMonitor.getCrashEvent();
                List<Peer> peers = activePeers.getPeers();
                CrashResponses responses = new CrashResponses(peers.size() - 1);
                // Send out notifications to all active peers in parallel
                for (Peer peer : peers) {
                    if (peer.getId() == crashEvent.getCrashedRobotId()) {
                        continue;
                    }
                    Thread thread = new Thread(new PeerNotifier(peer.getId(), crashEvent, responses));
                    thread.start();
                    thread.join();
                }
                responses.waitForResponses();
                // Examine responses
                List<Boolean> responsesList = responses.getResponses();
                if (responsesList.stream().allMatch(b -> b)) {
                    // Peer removal triggers notification on P2PServiceManager to remove channels and stubs
                    boolean removed = activePeers.removePeer(crashEvent.getCrashedRobotId());
                    if (removed) {
                        MessagePrinter.printMessage(
                                "Robot " + crashEvent.getCrashedRobotId() +
                                        " crashed and was removed from the list" +
                                        " of active peers.\nCurrent active peers: " + activePeers.getIds(),
                                MessagePrinter.ERROR_FORMAT,
                                true);
                        // Notify the server to remove the crashed robot
                        RestServiceManager.getInstance(null).deleteRobot(crashEvent.getCrashedRobotId(),
                                false);
                        // Evaluate if there is need for load balancing
                        LoadBalancingMonitor.getInstance().setState(LoadBalancingMonitor.LoadBalancingState.EVALUATING);
                    }
                } else {
                    MessagePrinter.printMessage(
                            "Crash event of robot " + crashEvent.getCrashedRobotId() +
                                    " was not confirmed by all peers. The robot will not be removed from the list" +
                                    " of active peers.",
                            MessagePrinter.ERROR_FORMAT,
                            true
                    );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
