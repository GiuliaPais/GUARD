package io.github.giuliapais.robotsnetwork.core;

import io.github.giuliapais.simulators.Measurement;
import io.github.giuliapais.simulators.PM10Simulator;
import io.github.giuliapais.utils.MessagePrinter;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public class MockSensorComponent extends Thread {
    private final String MQTT_BROKER_ADDRESS = "tcp://localhost:1883";
    private volatile boolean stop = false;

    private int robotId;

    private final SensorReadingsBuffer buffer = new SensorReadingsBuffer();
    private final PM10Simulator pm10Simulator = new PM10Simulator(buffer);
    private AveragesPublisher averagesPublisher;
    private final List<Double> averages = new ArrayList<>();
    private MqttAsyncClient mqttClient;
    private MessagePrinter messagePrinter;

    public MockSensorComponent() {
        this.averagesPublisher = new AveragesPublisher(averages);
    }

    private void initMqttClient() {
        // Initialize MQTT client
        try {
            mqttClient = new MqttAsyncClient(MQTT_BROKER_ADDRESS, MqttAsyncClient.generateClientId());
            averagesPublisher.setMqttClient(mqttClient);
        } catch (Exception e) {
            throw new RuntimeException("Error while trying to initialize the MQTT client", e);
        }
    }

    public void setMessagePrinter(MessagePrinter messagePrinter) {
        this.messagePrinter = messagePrinter;
        averagesPublisher.setMessagePrinter(messagePrinter);
    }

    public void stopMeGently() {
        stop = true;
    }

    public void setDistrict(int district) {
        if (!averagesPublisher.isAlive()) {
            averagesPublisher.setDistrict(district);
        } else {
            averagesPublisher.stopMeGently();
            averagesPublisher = new AveragesPublisher(averages);
            averagesPublisher.setDistrict(district);
            averagesPublisher.setRobotId(robotId);
            averagesPublisher.setMessagePrinter(messagePrinter);
            averagesPublisher.setMqttClient(mqttClient);
            averagesPublisher.start();
        }
    }

    public void setRobotId(int robotId) {
        this.robotId = robotId;
        averagesPublisher.setRobotId(robotId);
    }

    private double computeAverage(List<Measurement> measurements) {
        OptionalDouble avg = measurements
                .stream()
                .mapToDouble(Measurement::getValue)
                .average();
        return avg.orElse(-1.0);
    }

    @Override
    public void run() {
        messagePrinter.printMessage(
                "Starting MQTT client at " + MQTT_BROKER_ADDRESS + "...",
                MessagePrinter.INFO_FORMAT, true);
        initMqttClient();
        pm10Simulator.start();
        messagePrinter.printMessage(
                "Sensors started",
                MessagePrinter.INFO_FORMAT, true);
        averagesPublisher.start();
        List<Measurement> localCopy;
        while (!stop) {
            try {
                synchronized (buffer) {
                    buffer.wait();
                    localCopy = buffer.readAllAndClean();
                }
                double average = computeAverage(localCopy);
                messagePrinter.printMessage("Computed average: " + average,
                        MessagePrinter.INFO_FORMAT, true);
                synchronized (averages) {
                    averages.add(average);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupt received", e);
            }
        }
    }

}
