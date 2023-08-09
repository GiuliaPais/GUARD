package io.github.giuliapais.robotsnetwork.comm;

public abstract class AbstractCrashEventHandler extends Thread {
    final ActivePeers activePeers = ActivePeers.getInstance();
    final CrashEventMonitor crashEventsMonitor;

    public AbstractCrashEventHandler(CrashEventMonitor crashEventsMonitor) {
        this.crashEventsMonitor = crashEventsMonitor;
    }
}
