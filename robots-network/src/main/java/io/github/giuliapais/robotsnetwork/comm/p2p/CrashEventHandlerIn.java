package io.github.giuliapais.robotsnetwork.comm.p2p;

import io.github.giuliapais.robotsnetwork.comm.CrashEvent;
import io.github.giuliapais.robotsnetwork.comm.CrashRecoveryGrpc;
import io.github.giuliapais.robotsnetwork.comm.PingMessage;
import io.github.giuliapais.commons.MessagePrinter;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.HashMap;

public class CrashEventHandlerIn extends AbstractCrashEventHandler {
    public CrashEventHandlerIn(HashMap<Integer, ManagedChannel> channels) {
        super(CrashEventMonitor.getInstance(CrashEventMonitor.CrashEventMonitorType.INCOMING), channels);
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            CrashEvent crashEvent = null;
            try {
                // Peer removal triggers notification on P2PServiceManager to remove channels and stubs
                crashEvent = crashEventsMonitor.getCrashEvent();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                // Try pinging the peer to check if it's still alive
                CrashRecoveryGrpc.CrashRecoveryBlockingStub stub = stubs.get(crashEvent.getCrashedRobotId());
                if (stub == null) {
                    if (channels.get(crashEvent.getCrashedRobotId()) == null) {
                        return;
                    }
                    stub = CrashRecoveryGrpc.newBlockingStub(channels.get(crashEvent.getCrashedRobotId()));
                    stubs.put(crashEvent.getCrashedRobotId(), stub);
                }
                PingMessage pingOut = PingMessage.newBuilder().build();
                stub.pingRobot(pingOut);
                crashEventsMonitor.addPing(crashEvent.getCrashedRobotId(), false);
            } catch (StatusRuntimeException e) {
                Status.Code code = e.getStatus().getCode();
                if (code == Status.Code.UNAVAILABLE || code == Status.Code.UNKNOWN ||
                        code == Status.Code.INTERNAL ||
                        code == Status.Code.DEADLINE_EXCEEDED || code == Status.Code.ABORTED ||
                        code == Status.Code.CANCELLED) {
                    boolean removed = activePeers.removePeer(crashEvent.getCrashedRobotId());
                    if (removed) {
                        MessagePrinter.printMessage(
                                "Crash event received from robot " + crashEvent.getRobotId() + " : " +
                                        "Robot " + crashEvent.getCrashedRobotId() +
                                        " crashed and was removed from the list" +
                                        " of active peers.\nCurrent active peers: " + activePeers.getIds(),
                                MessagePrinter.ERROR_FORMAT,
                                true);
                    }
                    crashEventsMonitor.addPing(crashEvent.getCrashedRobotId(), true);
                }
            }
        }
    }
}
