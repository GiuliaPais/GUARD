package io.github.giuliapais.robotsnetwork.comm;

import io.github.giuliapais.utils.MessagePrinter;

public class CrashEventHandlerIn extends AbstractCrashEventHandler {
    public CrashEventHandlerIn() {
        super(CrashEventMonitor.getInstance(CrashEventMonitor.CrashEventMonitorType.INCOMING));
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Peer removal triggers notification on P2PServiceManager to remove channels and stubs
                CrashEvent crashEvent = crashEventsMonitor.getCrashEvent();
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
