package io.github.giuliapais.robotsnetwork.comm.p2p;

import io.github.giuliapais.robotsnetwork.comm.CrashRecoveryGrpc;
import io.grpc.ManagedChannel;

import java.util.HashMap;

public abstract class AbstractCrashEventHandler extends Thread {
    final ActivePeers activePeers = ActivePeers.getInstance();
    final CrashEventMonitor crashEventsMonitor;
    final HashMap<Integer, ManagedChannel> channels;
    final HashMap<Integer, CrashRecoveryGrpc.CrashRecoveryBlockingStub> stubs = new HashMap<>();

    public AbstractCrashEventHandler(CrashEventMonitor crashEventsMonitor,
                                     HashMap<Integer, ManagedChannel> channels) {
        this.crashEventsMonitor = crashEventsMonitor;
        this.channels = channels;
    }
}
