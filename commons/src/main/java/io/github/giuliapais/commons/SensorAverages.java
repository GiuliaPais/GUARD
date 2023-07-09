package io.github.giuliapais.commons;

import java.util.List;

public class SensorAverages {
    private int robotId;
    private long timestamp;
    private List<Double> averages;

    public SensorAverages() {
    }

    public SensorAverages(int robotId, long timestamp, List<Double> averages) {
        this.robotId = robotId;
        this.timestamp = timestamp;
        this.averages = averages;
    }

    public int getRobotId() {
        return robotId;
    }

    public void setRobotId(int robotId) {
        this.robotId = robotId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<Double> getAverages() {
        return averages;
    }

    public void setAverages(List<Double> averages) {
        this.averages = averages;
    }
}
