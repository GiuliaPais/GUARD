package io.github.giuliapais.robotsnetwork.comm.p2p;

import io.github.giuliapais.robotsnetwork.comm.CrashEvent;
import io.github.giuliapais.robotsnetwork.comm.CrashEventResponse;
import io.github.giuliapais.robotsnetwork.comm.CrashRecoveryGrpc;
import io.grpc.stub.StreamObserver;

public class CrashRecoveryImpl extends CrashRecoveryGrpc.CrashRecoveryImplBase {
    private final LogicalClock logicalClock;

    public CrashRecoveryImpl(int robotId) {
        this.logicalClock = LogicalClock.getInstance(robotId);
    }

    @Override
    public void notifyCrashEvent(CrashEvent request, StreamObserver<CrashEventResponse> responseObserver) {
        logicalClock.compareAndAdjust(request.getTimestamp());
        CrashEventMonitor.getInstance(CrashEventMonitor.CrashEventMonitorType.INCOMING).addCrashEvent(request);
        // Wait to know the ping result
        CrashEventResponse response = null;
        try {
            boolean hasCrashed = CrashEventMonitor.getInstance(CrashEventMonitor.CrashEventMonitorType.INCOMING)
                    .getPing(request.getCrashedRobotId());
            if (hasCrashed) {
                response = CrashEventResponse.newBuilder()
                        .setRobotId(logicalClock.getRobotId())
                        .setCrashedRobotId(request.getCrashedRobotId())
                        .setIsCrashed(true)
                        .setTimestamp(logicalClock.incrementAndGet())
                        .build();
            } else {
                response = CrashEventResponse.newBuilder()
                        .setRobotId(logicalClock.getRobotId())
                        .setCrashedRobotId(request.getCrashedRobotId())
                        .setIsCrashed(false)
                        .setTimestamp(logicalClock.incrementAndGet())
                        .build();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
