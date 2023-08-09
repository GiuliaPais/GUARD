package io.github.giuliapais.robotsnetwork.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giuliapais.commons.SensorAverages;
import io.github.giuliapais.utils.MessagePrinter;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.HashMap;
import java.util.List;

public class AveragesPublisher extends Thread {
    private static final long PUBLISHING_PERIOD = 15000;
    private String topic;
    private int robotId;
    private volatile boolean stop = false;
    private MessagePrinter messagePrinter;

    private MqttAsyncClient client;

    private final List<Double> averages;

    public AveragesPublisher(List<Double> averages) {
        this.averages = averages;
    }

    public void stopMeGently() {
        stop = true;
    }

    public void setMessagePrinter(MessagePrinter messagePrinter) {
        this.messagePrinter = messagePrinter;
    }

    public void setDistrict(int district) {
        this.topic = "greenfield/pollution/district" + district;
    }

    public void setMqttClient(MqttAsyncClient client) {
        this.client = client;
    }

    public void setRobotId(int robotId) {
        this.robotId = robotId;
    }

    private void connectToClient() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        HashMap<String, String> will = new HashMap<>();
        will.put("robotId", String.valueOf(robotId));
        will.put("status", "offline");
        ObjectMapper mapper = new ObjectMapper();
        try {
            String payload = mapper.writeValueAsString(will);
            options.setWill(topic, payload.getBytes(), 2, false);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while trying to serialize last will", e);
        }
        try {
            client.connect(options).waitForCompletion();
        } catch (MqttException e) {
            throw new RuntimeException("Error while trying to connect to MQTT broker", e);
        }
        messagePrinter.printMessage("Connected to MQTT broker", MessagePrinter.INFO_FORMAT, true);
    }

    private void subscribeToTopic() {
        try {
            client.subscribe(topic, 1).waitForCompletion();
        } catch (MqttException e) {
            throw new RuntimeException("Error while trying to subscribe to topic", e);
        }
        messagePrinter.printMessage("Subscribed to topic " + topic, MessagePrinter.INFO_FORMAT, true);
    }

    private void publishMessage() {
        List<Double> copied;
        synchronized (averages) {
            copied = List.copyOf(averages);
            averages.clear();
        }
        SensorAverages message = new SensorAverages(this.robotId, System.currentTimeMillis(), copied);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String payload = mapper.writeValueAsString(message);
            client.publish(topic, payload.getBytes(), 1, false);
        } catch (JsonProcessingException | MqttException e) {
            throw new RuntimeException("Error while trying to publish message", e);
        }
    }

    @Override
    public void run() {
        connectToClient();
        subscribeToTopic();
        while (!stop) {
            try {
                Thread.sleep(PUBLISHING_PERIOD);
                publishMessage();
            } catch (InterruptedException e) {
                throw new RuntimeException("Received interrupt", e);
            }
        }
        // Unsubscribe from topic and disconnect from MQTT broker
        try {
            client.unsubscribe(topic);
            client.disconnect();
        } catch (MqttException e) {
            throw new RuntimeException("Error while trying to unsubscribe/disconnect from MQTT broker", e);
        }
        messagePrinter.printMessage(
                "Unsubscribed from topic " + topic + " and disconnected from MQTT broker",
                MessagePrinter.INFO_FORMAT,
                true
        );
    }
}
