package io.github.giuliapais.robotsnetwork.comm;

import io.github.giuliapais.commons.DistrictBalancer;
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
        logicalClock.compareAndAdjust(request.getTimestamp());
        LoadBalancingMonitor.LoadBalancingState state = loadBalancingMonitor.getState();
        LoadBalancingResponse response;
        if (state == LoadBalancingMonitor.LoadBalancingState.STEADY ||
                (state == LoadBalancingMonitor.LoadBalancingState.EVALUATING &
                        logicalClock.compareTimestamps(request.getTimestamp(),
                                loadBalancingMonitor.getRequestTimestamp(), request.getRobotId()) > 0)) {
            response = LoadBalancingResponse.newBuilder()
                    .setRobotId(logicalClock.getRobotId())
                    .setTimestamp(logicalClock.incrementAndGet())
                    .setDistrict(districtBalancer.getDistrict(logicalClock.getRobotId()))
                    .setAllowed(true)
                    .build();
        } else {
            // If the current process is balancing or it's evaluating and the other process has a higher timestamp,
            // the request is denied
            response = LoadBalancingResponse.newBuilder()
                    .setRobotId(logicalClock.getRobotId())
                    .setTimestamp(logicalClock.incrementAndGet())
                    .setDistrict(districtBalancer.getDistrict(logicalClock.getRobotId()))
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
        HashMap<Integer, Integer> changesMap = new HashMap<>();
        for (LoadBalancingTerminationMessage.Change change : changes) {
            changesMap.put(change.getRobotId(), change.getNewDistrict());
        }
        districtBalancer.changeDistrict(changesMap);
        LoadBalancingTerminationAck response;
        if (changesMap.containsKey(logicalClock.getRobotId())) {
            int[] newPos = districtBalancer.getPosInDistrict(districtBalancer.getDistrict(logicalClock.getRobotId()));
            response = LoadBalancingTerminationAck.newBuilder()
                    .setRobotId(logicalClock.getRobotId())
                    .setTimestamp(logicalClock.incrementAndGet())
                    .setX(newPos[0])
                    .setY(newPos[1])
                    .build();
        } else {
            response = LoadBalancingTerminationAck.newBuilder()
                    .setRobotId(logicalClock.getRobotId())
                    .setTimestamp(logicalClock.incrementAndGet())
                    .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
