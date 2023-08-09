package io.github.giuliapais.robotsnetwork.comm;

import io.grpc.stub.StreamObserver;

public class CrashRecoveryImpl extends CrashRecoveryGrpc.CrashRecoveryImplBase {
    private final LogicalClock logicalClock;

    public CrashRecoveryImpl(int robotId) {
        this.logicalClock = LogicalClock.getInstance(robotId);
    }

    @Override
    public void notifyCrashEvent(CrashEvent request, StreamObserver<NoResponse> responseObserver) {
        logicalClock.compareAndAdjust(request.getTimestamp());
        CrashEventMonitor.getInstance(CrashEventMonitor.CrashEventMonitorType.INCOMING).addCrashEvent(request);
        responseObserver.onNext(NoResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
