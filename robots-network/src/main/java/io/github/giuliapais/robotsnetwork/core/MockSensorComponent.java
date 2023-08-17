package io.github.giuliapais.robotsnetwork.core;

import io.github.giuliapais.simulators.Measurement;
import io.github.giuliapais.simulators.PM10Simulator;
import io.github.giuliapais.utils.MessagePrinter;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public class MockSensorComponent extends Thread {


    private final SensorReadingsBuffer buffer = new SensorReadingsBuffer();
    private final PM10Simulator pm10Simulator = new PM10Simulator(buffer);
    private final AveragesPublisher averagesPublisher;
    private final List<Double> averages = new ArrayList<>();


    public MockSensorComponent(int robotId, int district) {
        this.averagesPublisher = new AveragesPublisher(averages, district, robotId);
    }

    private double computeAverage(List<Measurement> measurements) {
        OptionalDouble avg = measurements
                .stream()
                .mapToDouble(Measurement::getValue)
                .average();
        return avg.orElse(-1.0);
    }

    private void stopEverything() {
        pm10Simulator.stopMeGently();
        averagesPublisher.stopGently();
        MessagePrinter.printMessage(
                "Sensors stopping",
                MessagePrinter.INFO_FORMAT, true);
    }

    @Override
    public void run() {
        pm10Simulator.start();
        MessagePrinter.printMessage(
                "Sensors started",
                MessagePrinter.INFO_FORMAT, true);
        averagesPublisher.start();
        List<Measurement> localCopy;
        while (!interrupted()) {
            try {
                synchronized (buffer) {
                    buffer.wait();
                    localCopy = buffer.readAllAndClean();
                }
                double average = computeAverage(localCopy);
                synchronized (averages) {
                    averages.add(average);
                }
            } catch (InterruptedException e) {
                stopEverything();
            }
        }
        stopEverything();
    }
}
