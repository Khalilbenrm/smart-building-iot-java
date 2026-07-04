package com.smariot.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT client shared by the sensor-facing ingestion path: the app process subscribes
 * with it (see SensorDataMqttSubscriber), the simulator process publishes with it.
 * Replaces the previous per-reading synchronous HTTP call with a lightweight
 * publish/subscribe transport that scales to many concurrent sensors and keeps
 * working (via broker-side queueing/QoS) even if the app is briefly unavailable.
 */
@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id-prefix}")
    private String clientIdPrefix;

    @Bean(destroyMethod = "disconnect")
    public MqttClient mqttClient() throws MqttException {
        String clientId = clientIdPrefix + "-" + MqttClient.generateClientId();
        MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        client.connect(options);
        log.info("Connected MQTT client '{}' to broker {}", clientId, brokerUrl);
        return client;
    }
}
