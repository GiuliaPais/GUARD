package io.github.giuliapais.adminserver;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class PollutionMonitor extends Thread {
    private final String MQTT_BROKER_ADDRESS = "tcp://localhost:1883";
    private volatile boolean stop = false;
    private MqttClient mqttClient;

    private void initMqttClient() throws MqttException {
        mqttClient = new MqttClient(MQTT_BROKER_ADDRESS,
                MqttClient.generateClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        // Set the callbacks
        mqttClient.setCallback(new PollutionMonitorCallback());
        mqttClient.connect(options);
    }


    @Override
    public void run() {
        super.run();
    }
}
