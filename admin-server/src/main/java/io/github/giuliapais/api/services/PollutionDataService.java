package io.github.giuliapais.api.services;

import io.github.giuliapais.commons.models.SensorAverages;
import io.github.giuliapais.structures.PollutionDataStore;

public class PollutionDataService {
    private static volatile PollutionDataService instance;
    private final PollutionDataStore pollutionDataStore = new PollutionDataStore();

    private PollutionDataService() {
    }

    public static PollutionDataService getInstance() {
        if (instance == null) {
            synchronized (PollutionDataService.class) {
                if (instance == null) {
                    instance = new PollutionDataService();
                }
            }
        }
        return instance;
    }

    public void addSensorData(SensorAverages data) {
        pollutionDataStore.addData(data.getRobotId(), data.getTimestamp(), data.getAverages());
    }

    public double getAverage(int robotId, int n) {
        return pollutionDataStore.getAverageOfLastNReadings(robotId, n);
    }

    public double getAverageBetweenTimestamps(long t1, long t2) {
        return pollutionDataStore.getAverageBetweenTimestamps(t1, t2);
    }

}
