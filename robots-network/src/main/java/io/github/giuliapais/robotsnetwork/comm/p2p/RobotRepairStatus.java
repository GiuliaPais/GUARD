package io.github.giuliapais.robotsnetwork.comm.p2p;

public class RobotRepairStatus {
    public enum RepairStatus {
        FUNCTIONING,
        NEEDS_REPAIR,
        REPAIRING
    }

    private RepairStatus status;
    private long request_timestamp = Long.MAX_VALUE;

    // Used during monitoring of process that is accessing the resource to detect crashes
    private volatile int robotAccessingId = -1;
    private volatile String robotAccessingStatus = "clear";

    public RobotRepairStatus() {
        this.status = RepairStatus.FUNCTIONING;
    }

    public synchronized void needsRepairs(long timestamp) {
        if (this.status == RepairStatus.FUNCTIONING) {
            this.status = RepairStatus.NEEDS_REPAIR;
            this.request_timestamp = timestamp;
        }
    }

    public synchronized void setRepairing() {
        if (this.status == RepairStatus.NEEDS_REPAIR) {
            this.status = RepairStatus.REPAIRING;
        }
    }

    public synchronized void waitForRepairFinish() throws InterruptedException {
        while (this.status != RepairStatus.FUNCTIONING) {
            wait();
        }
    }

    public synchronized void repairFinished() {
        if (this.status == RepairStatus.REPAIRING) {
            this.status = RepairStatus.FUNCTIONING;
            this.request_timestamp = Long.MAX_VALUE;
            notifyAll();
        }
    }

    public synchronized RepairStatus getStatus() {
        return status;
    }

    public synchronized long getRequestTimestamp() {
        return request_timestamp;
    }

    public synchronized void setRobotAccessingId(int robotId) {
        this.robotAccessingId = robotId;
    }

    public synchronized int getRobotAccessingId() {
        return robotAccessingId;
    }

    public synchronized void setRobotAccessingStatus(String status) {
        this.robotAccessingStatus = status;
    }

    public synchronized String getRobotAccessingStatus() {
        return robotAccessingStatus;
    }
}
