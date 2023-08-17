package io.github.giuliapais.structures;

import java.util.*;

public class PollutionDataStore {
    private final Map<Integer, TreeMap<Long, DataEntry>> robotDataMap = new HashMap<>();

    private record DataEntry(List<Double> pollutionData) {

        public double getSum(int n, boolean descending) {
            if (n >= size()) {
                return pollutionData.stream().mapToDouble(Double::doubleValue).sum();
            } else {
                return descending
                        ? pollutionData.stream().skip(size() - n).mapToDouble(Double::doubleValue).sum()
                        : pollutionData.stream().limit(n).mapToDouble(Double::doubleValue).sum();
            }
        }

        public int size() {
            return pollutionData.size();
        }
    }

    public synchronized void addData(int robotId, long timestamp, List<Double> pollutionData) {
        robotDataMap
                .computeIfAbsent(robotId, k -> new TreeMap<>())
                .put(timestamp, new DataEntry(pollutionData));
    }

    public synchronized double getAverageOfLastNReadings(int robotId, int n) {
        TreeMap<Long, DataEntry> robotData = robotDataMap.get(robotId);
        if (robotData == null) {
            return -1; // No data available for the robot
        }

        double sum = 0;
        int count = 0;
        int entrySize;
        Iterator<Map.Entry<Long, DataEntry>> descendingIterator = robotData.descendingMap().entrySet().iterator();

        while (descendingIterator.hasNext() && count < n) {
            DataEntry entry = descendingIterator.next().getValue();
            entrySize = entry.size();
            sum += entry.getSum(n - count, true);
            if (entrySize > (n - count)) {
                count += (n - count);
            } else {
                count += entrySize;
            }
        }

        return count > 0 ? sum / count : 0;
    }

    public synchronized double getAverageBetweenTimestamps(long t1, long t2) {
        double sum = 0;
        int count = 0;

        for (TreeMap<Long, DataEntry> robotData : robotDataMap.values()) {
            NavigableMap<Long, DataEntry> subMap = robotData.subMap(t1, true, t2, true);
            for (DataEntry entry : subMap.values()) {
                sum += entry.getSum(entry.size(), false);
                count += entry.size();
            }
        }

        return count > 0 ? sum / count : 0;
    }
}
