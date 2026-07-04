package com.smariot.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smariot.model.dto.SensorDataDto;
import com.smariot.service.SensorDataService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Subscribes to sensor telemetry published over MQTT (iot/sensors/{deviceId}/data) and
 * feeds it into the same validate -> Kafka pipeline the REST endpoint uses. This is the
 * primary ingestion path for real/simulated sensors; the REST endpoint remains available
 * for manual/one-off ingestion. Disabled under the "simulator" profile so a simulator
 * process only publishes and doesn't also re-ingest its own readings.
 */
@Slf4j
@Component
@Profile("!simulator")
public class SensorDataMqttSubscriber implements MqttCallbackExtended {

    private final MqttClient mqttClient;
    private final SensorDataService sensorDataService;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.topic-filter}")
    private String topicFilter;

    @Value("${mqtt.qos}")
    private int qos;

    public SensorDataMqttSubscriber(MqttClient mqttClient,
                                     SensorDataService sensorDataService,
                                     @Qualifier("kafkaObjectMapper") ObjectMapper objectMapper) {
        this.mqttClient = mqttClient;
        this.sensorDataService = sensorDataService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws MqttException {
        mqttClient.setCallback(this);
        subscribe();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT connection {} to {}", reconnect ? "re-established" : "established", serverURI);
        if (reconnect) {
            try {
                subscribe();
            } catch (MqttException e) {
                log.error("Failed to re-subscribe to '{}' after MQTT reconnect", topicFilter, e);
            }
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost, automatic reconnect will retry", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            SensorDataDto dto = objectMapper.readValue(message.getPayload(), SensorDataDto.class);
            sensorDataService.ingestSensorData(dto);
            log.info("Ingested MQTT reading from topic '{}' for device {}", topic, dto.getDeviceId());
        } catch (Exception e) {
            log.error("Failed to process MQTT message on topic '{}', discarding payload: {}",
                    topic, new String(message.getPayload()), e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No outbound publishing performed by this subscriber.
    }

    private void subscribe() throws MqttException {
        mqttClient.subscribe(topicFilter, qos);
        log.info("Subscribed to MQTT topic filter '{}' (qos={})", topicFilter, qos);
    }
}
