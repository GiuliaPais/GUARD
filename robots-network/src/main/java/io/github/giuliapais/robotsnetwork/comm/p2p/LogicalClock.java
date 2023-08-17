package io.github.giuliapais.robotsnetwork.comm.p2p;

import java.util.Random;

public class LogicalClock {

    private static LogicalClock instance;
    private int robotId;
    private int clockSkew;
    private long logicalClock = 0;

    private LogicalClock(int robotId) {
        this.robotId = robotId;
        Random random = new Random();
        this.clockSkew = random.nextInt(1000);
        this.logicalClock = 0;
    }

    public static LogicalClock getInstance(int robotId) {
        LogicalClock result = instance;
        if (result == null) {
            synchronized (LogicalClock.class) {
                result = instance;
                if (result == null) {
                    instance = result = new LogicalClock(robotId);
                }
            }
        }
        return result;
    }

    public synchronized long incrementAndGet() {
        return ++logicalClock + clockSkew;
    }

    public synchronized void compareAndAdjust(long otherClock) {
        logicalClock = Math.max(logicalClock, otherClock) + 1;
    }

    public int compareTimestamps(long timestamp1, long timestamp2, int robotId2) {
        if (timestamp1 == timestamp2) {
            return Integer.compare(robotId, robotId2);
        }
        return Long.compare(timestamp1, timestamp2);
    }

    public int getRobotId() {
        return robotId;
    }
}
