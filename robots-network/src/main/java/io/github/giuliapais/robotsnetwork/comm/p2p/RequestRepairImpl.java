package io.github.giuliapais.robotsnetwork.comm.p2p;

import io.github.giuliapais.robotsnetwork.comm.*;
import io.github.giuliapais.utils.MessagePrinter;
import io.grpc.stub.StreamObserver;


public class RequestRepairImpl extends RepairServiceGrpc.RepairServiceImplBase {
    private final RobotRepairStatus repairStatus;
    private final LogicalClock logicalClock;

    public RequestRepairImpl(RobotRepairStatus repairStatus, int robotId) {
        this.repairStatus = repairStatus;
        this.logicalClock = LogicalClock.getInstance(robotId);
    }

    @Override
    public void requestRepair(RepairRequest request, StreamObserver<RepairResponse> responseObserver) {
        logicalClock.compareAndAdjust(request.getTimestamp());
        try {
            if (repairStatus.getStatus() != RobotRepairStatus.RepairStatus.FUNCTIONING) {
                // Check the timestamps and precedence:
                // If the timestamp of the local request is lower than the received one, or if they are equal
                // but the local robot has a lower id, then the local robot has precedence
                int localGreater = Long.compare(repairStatus.getRequestTimestamp(), request.getTimestamp());
                if (localGreater < 0 || (localGreater == 0 && request.getRobotId() < logicalClock.getRobotId())) {
                    // Defer the request
                    repairStatus.waitForRepairFinish();
                }
            }
            // Send ack
            RepairResponse response = RepairResponse
                    .newBuilder()
                    .setAccepted(true)
                    .setTimestamp(logicalClock.incrementAndGet())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accessCrashDetection(AccessRequest request, StreamObserver<AccessResponse> responseObserver) {
        logicalClock.compareAndAdjust(request.getTimestamp());
        if (request.getType().equals("access")) {
            repairStatus.setRobotAccessingId(request.getRobotId());
            repairStatus.setRobotAccessingStatus("access");
            MessagePrinter.printMessage(
                    "Robot " + request.getRobotId() + " is accessing the resource, monitoring crash events",
                    MessagePrinter.ACCENT_FORMAT,
                    true);
            Thread crashDetectionThread = new Thread(() -> {
                try {
                    Thread.sleep(15000); // 15 seconds (repair time + margin)
                    if (repairStatus.getRobotAccessingId() == request.getRobotId() &
                            !repairStatus.getRobotAccessingStatus().equals("clear")) {
                        MessagePrinter.printMessage(
                                "Robot " + request.getRobotId() + " crashed while accessing the resource",
                                MessagePrinter.ERROR_FORMAT,
                                true);
                        CrashEventMonitor.getInstance(CrashEventMonitor.CrashEventMonitorType.OUTGOING)
                                .addCrashEvent(CrashEvent.newBuilder()
                                        .setRobotId(logicalClock.getRobotId())
                                        .setCrashedRobotId(request.getRobotId())
                                        .setTimestamp(logicalClock.incrementAndGet())
                                        .build());
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            crashDetectionThread.start();
        } else if (request.getType().equals("release")) {
            repairStatus.setRobotAccessingId(-1);
            repairStatus.setRobotAccessingStatus("clear");
            MessagePrinter.printMessage(
                    "Robot " + request.getRobotId() + " is releasing the resource, stopping monitoring",
                    MessagePrinter.ACCENT_FORMAT,
                    true);
        }

        // Send ack
        AccessResponse response = AccessResponse
                .newBuilder()
                .setRobotId(logicalClock.getRobotId())
                .setTimestamp(logicalClock.incrementAndGet())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
