package io.github.giuliapais.adminserver;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class PollutionMonitor {
    private final String MQTT_BROKER_ADDRESS = "tcp://localhost:1883";
    private MqttAsyncClient mqttClient;

    public PollutionMonitor() throws MqttException {
        initMqttClient();
    }

    private void initMqttClient() throws MqttException {
        mqttClient = new MqttAsyncClient(
                MQTT_BROKER_ADDRESS,
                "ADMIN-SERVER");
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        // Set the callbacks
        mqttClient.setCallback(new PollutionMonitorCallback());
        mqttClient.connect(options).waitForCompletion();
        subscribeToPollutionTopic();
    }

    private void subscribeToPollutionTopic() throws MqttException {
        mqttClient.subscribe("greenfield/pollution/#", 1).waitForCompletion();
    }

    public void disconnect() throws MqttException {
        mqttClient.unsubscribe("#");
        mqttClient.disconnect();
        mqttClient.close();
    }

}
