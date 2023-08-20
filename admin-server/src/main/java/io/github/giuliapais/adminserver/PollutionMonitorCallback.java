package io.github.giuliapais.adminserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giuliapais.api.services.PollutionDataService;
import io.github.giuliapais.commons.models.SensorAverages;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class PollutionMonitorCallback implements MqttCallback {
    @Override
    public void connectionLost(Throwable cause) {
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        PollutionDataService service = PollutionDataService.getInstance();
        String payload = new String(message.getPayload());
        SensorAverages averages = new ObjectMapper().readValue(payload, SensorAverages.class);
        service.addSensorData(averages);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
