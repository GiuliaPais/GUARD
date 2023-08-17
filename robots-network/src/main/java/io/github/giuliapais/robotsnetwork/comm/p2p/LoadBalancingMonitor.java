package io.github.giuliapais.robotsnetwork.comm.p2p;

public class LoadBalancingMonitor {

    private static LoadBalancingMonitor instance;
    private LoadBalancingState state;
    private long requestTimestamp = Long.MAX_VALUE;

    public enum LoadBalancingState {
        STEADY,
        EVALUATING,
        REBALANCING
    }

    private LoadBalancingMonitor() {
        state = LoadBalancingState.STEADY;
    }

    public static LoadBalancingMonitor getInstance() {
        LoadBalancingMonitor result = instance;
        if (result == null) {
            synchronized (LoadBalancingMonitor.class) {
                result = instance;
                if (result == null) {
                    instance = result = new LoadBalancingMonitor();
                }
            }
        }
        return result;
    }

    public synchronized LoadBalancingState getState() {
        return state;
    }

    public synchronized void setRequestTimestamp(long requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public synchronized Long getRequestTimestamp() {
        return requestTimestamp;
    }

    public synchronized LoadBalancingState waitForNotSteady() {
        while (state == LoadBalancingState.STEADY) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return state;
    }

    public synchronized void setState(LoadBalancingState state) {
        LoadBalancingState oldState = this.state;
        this.state = state;
        if (state == LoadBalancingState.STEADY) {
            this.requestTimestamp = Long.MAX_VALUE;
        }
        if ((oldState == LoadBalancingState.STEADY & state == LoadBalancingState.EVALUATING) ||
                (oldState == LoadBalancingState.EVALUATING & state == LoadBalancingState.REBALANCING)) {
            notifyAll();
        }
    }
}
