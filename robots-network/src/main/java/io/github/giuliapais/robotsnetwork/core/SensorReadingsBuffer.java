package io.github.giuliapais.robotsnetwork.core;

import io.github.giuliapais.simulators.Buffer;
import io.github.giuliapais.simulators.Measurement;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SensorReadingsBuffer implements Buffer {

    private static final int WINDOW_SIZE = 8;
    private static final int OVERLAP = WINDOW_SIZE / 2;

    private final LinkedList<Measurement> buffer = new LinkedList<>();

    @Override
    public synchronized void addMeasurement(Measurement m) {
        buffer.add(m);
        if (buffer.size() == WINDOW_SIZE) {
            this.notify();
        }
    }

    @Override
    public synchronized List<Measurement> readAllAndClean() {
        List<Measurement> allMeasurements = new ArrayList<>(buffer);
        for (int i = 0; i < OVERLAP; i++) {
            buffer.removeFirst();
        }
        return allMeasurements;
    }
}
