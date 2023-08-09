package io.github.giuliapais.robotsnetwork.comm;

import io.github.giuliapais.commons.DistrictBalancer;
import io.github.giuliapais.utils.MessagePrinter;
import io.grpc.stub.StreamObserver;

public class IntroductionImpl extends IntroduceMeGrpc.IntroduceMeImplBase {

    private final int robotId;
    private final ActivePeers activePeers;
    private final DistrictBalancer districtBalancer;

    public IntroductionImpl(int robotId, DistrictBalancer districtBalancer) {
        this.robotId = robotId;
        this.activePeers = ActivePeers.getInstance();
        this.districtBalancer = districtBalancer;
    }

    @Override
    public void introduceMe(IntroduceMeRequest request, StreamObserver<IntroduceMeResponse> responseObserver) {
        IntroduceMeResponse response = IntroduceMeResponse
                .newBuilder()
                .setRobotId(robotId)
                .setDistrict(districtBalancer.getDistrict(robotId))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        synchronized (activePeers) {
            activePeers.addPeer(new Peer(request.getRobotId(), request.getIpAddress(), request.getPort()));
        }
        districtBalancer.addRobot(request.getRobotId(), request.getDistrict());
        MessagePrinter.printMessage(
                "A new robot joined Greenfield! Welcome robot " + request.getRobotId() + "!",
                MessagePrinter.ACCENT_FORMAT,
                true);
    }
}
