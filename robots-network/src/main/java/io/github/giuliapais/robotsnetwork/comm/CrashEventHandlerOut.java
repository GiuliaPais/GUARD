package io.github.giuliapais.robotsnetwork.comm;

import io.github.giuliapais.utils.MessagePrinter;
import io.grpc.ManagedChannel;

import java.util.HashMap;

public class CrashEventHandlerOut extends AbstractCrashEventHandler {
    private final HashMap<Integer, ManagedChannel> channels;
    private final HashMap<Integer, CrashRecoveryGrpc.CrashRecoveryBlockingStub> stubs = new HashMap<>();

    public CrashEventHandlerOut(HashMap<Integer, ManagedChannel> channels) {
        super(CrashEventMonitor.getInstance(CrashEventMonitor.CrashEventMonitorType.OUTGOING));
        this.channels = channels;
    }

    // Dedicated thread to notify the peer
    class PeerNotifier implements Runnable {
        private final int peerId;
        private final CrashEvent crashEvent;

        public PeerNotifier(int peerId, CrashEvent crashEvent) {
            this.peerId = peerId;
            this.crashEvent = crashEvent;
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
                NoResponse response = stub.notifyCrashEvent(crashEvent);
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
                // Peer removal triggers notification on P2PServiceManager to remove channels and stubs
                boolean removed = activePeers.removePeer(crashEvent.getCrashedRobotId());
                if (removed) {
                    MessagePrinter.printMessage(
                            "Robot " + crashEvent.getCrashedRobotId() +
                                    " crashed and was removed from the list" +
                                    " of active peers.\nCurrent active peers: " + activePeers.getIds(),
                            MessagePrinter.ERROR_FORMAT,
                            true);
                    // Send out notifications to all active peers in parallel
                    for (Peer peer : activePeers.getPeers()) {
                        new Thread(new PeerNotifier(peer.getId(), crashEvent)).start();
                    }
                    // TODO:Send REST delete request to the server

                    // TODO:Call load balancing service
                    LoadBalancingMonitor.getInstance().setState(LoadBalancingMonitor.LoadBalancingState.EVALUATING);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
