package io.github.giuliapais.robotsnetwork.comm.p2p;

import io.github.giuliapais.commons.DistrictBalancer;
import io.github.giuliapais.commons.models.MapPosition;
import io.github.giuliapais.robotsnetwork.comm.IntroduceMeGrpc;
import io.github.giuliapais.robotsnetwork.comm.IntroduceMeRequest;
import io.github.giuliapais.robotsnetwork.comm.IntroduceMeResponse;
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
        MapPosition myPosition = districtBalancer.getRobotPosition(robotId);
        IntroduceMeResponse response = IntroduceMeResponse
                .newBuilder()
                .setRobotId(robotId)
                .setDistrict(myPosition.getDistrict())
                .setX(myPosition.getX())
                .setY(myPosition.getY())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        synchronized (activePeers) {
            activePeers.addPeer(new Peer(request.getRobotId(), request.getIpAddress(), request.getPort()));
        }
        districtBalancer.addRobot(request.getRobotId(),
                new MapPosition(request.getDistrict(), request.getX(), request.getY()));
        MessagePrinter.printMessage(
                "A new robot joined Greenfield! Welcome robot " + request.getRobotId() + "!",
                MessagePrinter.ACCENT_FORMAT,
                true);
    }
}
