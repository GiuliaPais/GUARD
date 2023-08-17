package io.github.giuliapais.robotsnetwork.comm.p2p;

import io.github.giuliapais.robotsnetwork.comm.GoodbyeAck;
import io.github.giuliapais.robotsnetwork.comm.GoodbyeMessage;
import io.github.giuliapais.robotsnetwork.comm.GracefulExitGrpc;
import io.github.giuliapais.utils.MessagePrinter;
import io.grpc.stub.StreamObserver;

public class GracefulExitImpl extends GracefulExitGrpc.GracefulExitImplBase {
    private final LogicalClock logicalClock;
    private final ActivePeers activePeers = ActivePeers.getInstance();

    public GracefulExitImpl(int robotId) {
        this.logicalClock = LogicalClock.getInstance(robotId);
    }

    @Override
    public void sayGoodbye(GoodbyeMessage request, StreamObserver<GoodbyeAck> responseObserver) {
        logicalClock.compareAndAdjust(request.getTimestamp());
        MessagePrinter.printMessage(
                "Robot " + request.getRobotId() + " is leaving Greenfield. Goodbye!",
                MessagePrinter.ACCENT_FORMAT,
                true
        );
        GoodbyeAck goodbyeAck = GoodbyeAck.newBuilder()
                .setRobotId(logicalClock.getRobotId())
                .setTimestamp(logicalClock.incrementAndGet())
                .build();
        responseObserver.onNext(goodbyeAck);
        responseObserver.onCompleted();
        // Remove the robot from the list of active peers
        activePeers.removePeer(request.getRobotId());
        // Evaluate load balancing
        LoadBalancingMonitor.getInstance().setState(LoadBalancingMonitor.LoadBalancingState.EVALUATING);
    }
}
