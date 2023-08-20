package io.github.giuliapais.robotsnetwork.comm.p2p;

import io.github.giuliapais.commons.DistrictBalancer;
import io.github.giuliapais.commons.models.MapPosition;
import io.github.giuliapais.robotsnetwork.comm.*;
import io.github.giuliapais.commons.MessagePrinter;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.List;

public class DistrictBalancingImpl extends DistrictBalancingGrpc.DistrictBalancingImplBase {

    private final LogicalClock logicalClock;
    private final LoadBalancingMonitor loadBalancingMonitor = LoadBalancingMonitor.getInstance();
    private final DistrictBalancer districtBalancer;

    public DistrictBalancingImpl(int robotId, DistrictBalancer districtBalancer) {
        this.logicalClock = LogicalClock.getInstance(robotId);
        this.districtBalancer = districtBalancer;
    }

    @Override
    public void loadBalancingInitiation(LoadBalancingRequest request,
                                        StreamObserver<LoadBalancingResponse> responseObserver) {
        MessagePrinter.printMessage(
                "Received load balancing initiation request from robot " +
                        request.getRobotId() + " with timestamp " +
                        request.getTimestamp(),
                MessagePrinter.WARNING_FORMAT, true
        );
        logicalClock.compareAndAdjust(request.getTimestamp());
        LoadBalancingMonitor.LoadBalancingState state = loadBalancingMonitor.getState();
        LoadBalancingResponse response;
        MapPosition myPosition = districtBalancer.getRobotPosition(logicalClock.getRobotId());
        if (state == LoadBalancingMonitor.LoadBalancingState.STEADY ||
                (state == LoadBalancingMonitor.LoadBalancingState.EVALUATING &
                        logicalClock.compareTimestamps(request.getTimestamp(),
                                loadBalancingMonitor.getRequestTimestamp(), request.getRobotId()) < 0)) {
            response = LoadBalancingResponse.newBuilder()
                    .setRobotId(logicalClock.getRobotId())
                    .setTimestamp(logicalClock.incrementAndGet())
                    .setDistrict(myPosition.getDistrict())
                    .setX(myPosition.getX())
                    .setY(myPosition.getY())
                    .setAllowed(true)
                    .build();
        } else {
            // If the current process is balancing or it's evaluating and the other process has a higher timestamp,
            // the request is denied
            response = LoadBalancingResponse.newBuilder()
                    .setRobotId(logicalClock.getRobotId())
                    .setTimestamp(logicalClock.incrementAndGet())
                    .setDistrict(myPosition.getDistrict())
                    .setX(myPosition.getX())
                    .setY(myPosition.getY())
                    .setAllowed(false)
                    .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void loadBalancingTermination(LoadBalancingTerminationMessage request,
                                         StreamObserver<LoadBalancingTerminationAck> responseObserver) {
        logicalClock.compareAndAdjust(request.getTimestamp());
        List<LoadBalancingTerminationMessage.Change> changes = request.getChangesList();
        HashMap<Integer, MapPosition> changesMap = new HashMap<>();
        for (LoadBalancingTerminationMessage.Change change : changes) {
            changesMap.put(change.getRobotId(), new MapPosition(change.getNewDistrict(), change.getNewX(),
                    change.getNewY()));
        }
        districtBalancer.updatePositions(changesMap);
        if (changesMap.containsKey(logicalClock.getRobotId())) {
            ChangeDistrictMonitor.getInstance()
                    .districtChanged(changesMap.get(logicalClock.getRobotId()).getDistrict());
        }
        LoadBalancingTerminationAck response = LoadBalancingTerminationAck.newBuilder()
                .setRobotId(logicalClock.getRobotId())
                .setTimestamp(logicalClock.incrementAndGet())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
