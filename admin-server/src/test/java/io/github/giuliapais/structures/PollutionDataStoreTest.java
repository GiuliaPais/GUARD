package io.github.giuliapais.structures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PollutionDataStoreTest {
    PollutionDataStore pollutionDataStore;

    @BeforeEach
    void setUp() {
        pollutionDataStore = new PollutionDataStore();
    }

    void populateDataStore() {
        // Robot 1 data
        pollutionDataStore.addData(1, 1000, List.of(1.0, 1.5, 2.5, 3.0));
        pollutionDataStore.addData(1, 2000, List.of(3.0, 2.8, 2.5, 3.1, 2.9));
        pollutionDataStore.addData(1, 3000, List.of(2.9, 2.8, 2.7, 2.6, 2.5));

        // Robot 2 data
        pollutionDataStore.addData(2, 1000, List.of(1.3, 1.1, 2.0, 1.9, 1.8));
        pollutionDataStore.addData(2, 2000, List.of(1.8, 1.9, 3.0, 2.9));
        pollutionDataStore.addData(2, 3000, List.of(2.6, 2.5, 1.9, 2.0, 2.1));
    }

    @Test
    void testNReadings() {
        populateDataStore();
        int n = 5;
        double expected1 = Stream.of(2.9, 2.8, 2.7, 2.6, 2.5).mapToDouble(Double::doubleValue)
                .average().orElse(0);
        double expected2 = Stream.of(2.6, 2.5, 1.9, 2.0, 2.1).mapToDouble(Double::doubleValue)
                .average().orElse(0);

        double res1 = pollutionDataStore.getAverageOfLastNReadings(1, n);
        double res2 = pollutionDataStore.getAverageOfLastNReadings(2, n);

        assertEquals(expected1, res1);
        assertEquals(expected2, res2);

        n = 7;
        expected1 = Stream.of(2.9, 2.8, 2.7, 2.6, 2.5, 3.1, 2.9).mapToDouble(Double::doubleValue)
                .average().orElse(0);
        expected2 = Stream.of(2.6, 2.5, 1.9, 2.0, 2.1, 3.0, 2.9).mapToDouble(Double::doubleValue)
                .average().orElse(0);
        res1 = pollutionDataStore.getAverageOfLastNReadings(1, n);
        res2 = pollutionDataStore.getAverageOfLastNReadings(2, n);

        assertEquals(expected1, res1);
        assertEquals(expected2, res2);
    }

    @Test
    void testTpAverages() {
        populateDataStore();
        double expected = Stream.of(
                        1.0, 1.5, 2.5, 3.0,
                        3.0, 2.8, 2.5, 3.1, 2.9,
                        1.3, 1.1, 2.0, 1.9, 1.8,
                        1.8, 1.9, 3.0, 2.9)
                .mapToDouble(Double::doubleValue).average().orElse(0);
        double avg = pollutionDataStore.getAverageBetweenTimestamps(1000, 2000);
        assertEquals(expected, avg);

        avg = pollutionDataStore.getAverageBetweenTimestamps(1000, 2500);
        assertEquals(expected, avg);

        expected = Stream.of(3.0, 2.8, 2.5, 3.1, 2.9,
                        1.8, 1.9, 3.0, 2.9)
                .mapToDouble(Double::doubleValue).average().orElse(0);
        avg = pollutionDataStore.getAverageBetweenTimestamps(1500, 2000);
        assertEquals(expected, avg);
        expected = Stream.of(
                        1.0, 1.5, 2.5, 3.0,
                        3.0, 2.8, 2.5, 3.1, 2.9,
                        2.9, 2.8, 2.7, 2.6, 2.5,
                        1.3, 1.1, 2.0, 1.9, 1.8,
                        1.8, 1.9, 3.0, 2.9,
                        2.6, 2.5, 1.9, 2.0, 2.1)
                .mapToDouble(Double::doubleValue).average().orElse(0);
        avg = pollutionDataStore.getAverageBetweenTimestamps(900, 3100);
        assertEquals(expected, avg);
    }
}