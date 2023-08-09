package io.github.giuliapais.robotsnetwork.core;

import io.github.giuliapais.robotsnetwork.comm.LogicalClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class LogicalClockTest {

    LogicalClock logicalClock;

    @BeforeEach
    void setUp() {
        logicalClock = LogicalClock.getInstance(1);
    }

    @Test
    void incrementAndGet() throws NoSuchFieldException, IllegalAccessException {
        // Before increment
        Field clockSkewField = logicalClock.getClass().getDeclaredField("clockSkew");
        clockSkewField.setAccessible(true);

        Field logicalClockField = logicalClock.getClass().getDeclaredField("logicalClock");
        logicalClockField.setAccessible(true);

        int clockSkew = (int) clockSkewField.get(logicalClock);
        long logicalClockValue = (long) logicalClockField.get(logicalClock);
        long valueBefore = clockSkew + logicalClockValue;

        // After increment
        long valueAfter = logicalClock.incrementAndGet();

        assertEquals(valueBefore + 1, valueAfter);
        assertEquals(clockSkew, clockSkewField.get(logicalClock));
    }

    @Test
    void compareAndAdjust() throws NoSuchFieldException, IllegalAccessException {
        Field clockSkewField = logicalClock.getClass().getDeclaredField("clockSkew");
        clockSkewField.setAccessible(true);
        clockSkewField.set(logicalClock, 0);

        Field logicalClockField = logicalClock.getClass().getDeclaredField("logicalClock");
        logicalClockField.setAccessible(true);
        logicalClockField.set(logicalClock, 0L);


        long otherClock = 100;
        logicalClock.compareAndAdjust(otherClock);
        assertEquals(otherClock + 1,
                (int) clockSkewField.get(logicalClock) + (long) logicalClockField.get(logicalClock));
    }

}