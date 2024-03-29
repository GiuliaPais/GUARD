package io.github.giuliapais.robotsnetwork.core;

import io.github.giuliapais.simulators.Measurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SensorReadingsBufferTest {
    private SensorReadingsBuffer buffer;
    private Measurement mockMeasurement;

    @BeforeEach
    public void setup() {
        buffer = new SensorReadingsBuffer();
        mockMeasurement = Mockito.mock(Measurement.class);
    }

    @Test
    public void testNotificationOnBufferFull() throws InterruptedException {
        final boolean[] wasNotified = new boolean[1];

        Thread waitingThread = new Thread(() -> {
            synchronized (buffer) {
                try {
                    Field bufferField = SensorReadingsBuffer.class.getDeclaredField("buffer");
                    bufferField.setAccessible(true);
                    LinkedList<Measurement> internalBuffer = (LinkedList<Measurement>) bufferField.get(buffer);

                    while (internalBuffer.size() < 8) {
                        try {
                            buffer.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    wasNotified[0] = true;
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread addingThread = new Thread(() -> {
            for (int i = 0; i < 8; i++) {
                buffer.addMeasurement(mockMeasurement);
            }
        });

        waitingThread.start();
        addingThread.start();

        addingThread.join();
        waitingThread.join();

        assertTrue(wasNotified[0]);
    }

    @Test
    public void testReadAllAndClean() throws NoSuchFieldException, IllegalAccessException {
        for (int i = 0; i < 8; i++) {
            buffer.addMeasurement(mockMeasurement);
        }

        List<Measurement> measurements = buffer.readAllAndClean();

        assertEquals(8, measurements.size());

        Field bufferField = SensorReadingsBuffer.class.getDeclaredField("buffer");
        bufferField.setAccessible(true);
        LinkedList<Measurement> internalBuffer = (LinkedList<Measurement>) bufferField.get(buffer);

        assertEquals(4, internalBuffer.size());
    }

    @Test
    public void testMorethanEight() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        OngoingStubbing<Double> stubbing = when(mockMeasurement.getValue());
        for (int i = 0; i < 10; i++) {
            stubbing = stubbing.thenReturn(i + 10.0);
        }
        Thread addingThread = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                buffer.addMeasurement(mockMeasurement);
            }
        });
        addingThread.start();
        synchronized (buffer) {
            buffer.wait();
        }
        List<Measurement> measurements = buffer.readAllAndClean();
        assertEquals(8, measurements.size());
        double sumFirst8 = 0;
        for (int i = 0; i < 8; i++) {
            sumFirst8 += (i + 10.0);
        }
        double sumList = 0;
        for (Measurement m : measurements) {
            sumList += m.getValue();
        }

        Field bufferField = SensorReadingsBuffer.class.getDeclaredField("buffer");
        bufferField.setAccessible(true);
        LinkedList<Measurement> internalBuffer = (LinkedList<Measurement>) bufferField.get(buffer);

        assertEquals(6, internalBuffer.size()); // 4 left (overlap) + 2 remaining
        assertEquals(sumList, sumFirst8);
    }
}