package io.github.giuliapais.robotsnetwork.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giuliapais.commons.models.SensorAverages;
import io.github.giuliapais.robotsnetwork.comm.p2p.ChangeDistrictMonitor;
import io.github.giuliapais.commons.MessagePrinter;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;

public class AveragesPublisher extends Thread {
    private final String MQTT_BROKER_ADDRESS = "tcp://localhost:1883";
    private static final long PUBLISHING_PERIOD = 15000;

    private static volatile boolean stop = false;

    private String topic;
    private int robotId;
    private MqttAsyncClient client;
    private volatile Integer newDistrict = 0;

    private final List<Double> averages;

    private static class ChangeDistrictHandler extends Thread {
        private final ChangeDistrictMonitor monitor = ChangeDistrictMonitor.getInstance();
        private final AveragesPublisher publisher;

        public ChangeDistrictHandler(AveragesPublisher publisher) {
            this.publisher = publisher;
        }

        @Override
        public void run() {
            while (!interrupted()) {
                try {
                    int district = monitor.monitorChanges();
                    publisher.signalDistrictChanged(district);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public AveragesPublisher(List<Double> averages, int district, int robotId) {
        this.averages = averages;
        this.robotId = robotId;
        setDistrict(district);
    }

    private void initMqttClient() {
        // Initialize MQTT client
        try {
            client = new MqttAsyncClient(
                    MQTT_BROKER_ADDRESS,
                    "ROBOT-" + robotId);
        } catch (Exception e) {
            throw new RuntimeException("Error while trying to initialize the MQTT client", e);
        }
    }

    private void setDistrict(int district) {
        this.topic = "greenfield/pollution/district" + district;
    }

    void signalDistrictChanged(Integer newDistrict) {
        this.newDistrict = newDistrict;
    }

    private void changeDistrict() throws MqttException {
        int districtId;
        districtId = newDistrict;
        newDistrict = 0;
        // Unsubscribe from old topic
        client.unsubscribe(topic);
        // Subscribe to new topic
        setDistrict(districtId);
        subscribeToTopic();
    }

    private void connectToClient() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
//        HashMap<String, String> will = new HashMap<>();
//        will.put("robotId", String.valueOf(robotId));
//        will.put("status", "offline");
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            String payload = mapper.writeValueAsString(will);
//            options.setWill(topic, payload.getBytes(), 2, false);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException("Error while trying to serialize last will", e);
//        }
        try {
            client.connect(options).waitForCompletion();
        } catch (MqttException e) {
            throw new RuntimeException("Error while trying to connect to MQTT broker", e);
        }
        MessagePrinter.printMessage("Connected to MQTT broker", MessagePrinter.INFO_FORMAT, true);
    }

    private void subscribeToTopic() {
        try {
            client.subscribe(topic, 1).waitForCompletion();
        } catch (MqttException e) {
            throw new RuntimeException("Error while trying to subscribe to topic", e);
        }
        MessagePrinter.printMessage("Subscribed to topic " + topic, MessagePrinter.INFO_FORMAT, true);
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

    private void stopEverything(ChangeDistrictHandler handler) {
        handler.interrupt();
        try {
            client.unsubscribe(topic).waitForCompletion();
            client.disconnect().waitForCompletion();
            client.close();
        } catch (MqttException e) {
            throw new RuntimeException("Error while trying to unsubscribe/disconnect from MQTT broker", e);
        }
        MessagePrinter.printMessage(
                "Unsubscribed from topic " + topic + " and disconnected from MQTT broker",
                MessagePrinter.INFO_FORMAT,
                true
        );
    }

    public void stopGently() {
        stop = true;
    }

    @Override
    public void run() {
        MessagePrinter.printMessage(
                "Starting MQTT client at " + MQTT_BROKER_ADDRESS + "...",
                MessagePrinter.INFO_FORMAT, true);
        initMqttClient();
        ChangeDistrictHandler changeDistrictHandler = new ChangeDistrictHandler(this);
        changeDistrictHandler.start();
        connectToClient();
        subscribeToTopic();
        while (!stop) {
            try {
                if (newDistrict != 0) {
                    changeDistrict();
                }
                Thread.sleep(PUBLISHING_PERIOD);
                publishMessage();
            } catch (InterruptedException e) {
                stopEverything(changeDistrictHandler);
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
        // Unsubscribe from topic and disconnect from MQTT broker
        stopEverything(changeDistrictHandler);
    }
}
