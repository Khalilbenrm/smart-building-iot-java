package com.smariot.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smariot.model.dto.SensorDataDto;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Bonus: continuously generates realistic sensor readings for a handful of building
 * zones and publishes them over MQTT, the same lightweight publish/subscribe transport
 * real sensors use, instead of blocking on a synchronous HTTP call per reading. Enabled
 * only under the "simulator" profile so it never runs as part of normal application startup.
 *
 * Usage: java -jar smart-iot-building.jar --spring.profiles.active=simulator
 */
@Slf4j
@Component
@Profile("simulator")
public class IotSensorSimulator implements CommandLineRunner {

    private static final List<String> DEVICE_IDS = List.of(
            "office-01", "meeting-room-01", "corridor-01", "server-room-01", "hvac-critical-01");

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${mqtt.topic-prefix}")
    private String topicPrefix;

    @Value("${mqtt.qos}")
    private int qos;

    @Value("${simulator.interval-seconds:3}")
    private long intervalSeconds;

    public IotSensorSimulator(MqttClient mqttClient, @Qualifier("kafkaObjectMapper") ObjectMapper objectMapper) {
        this.mqttClient = mqttClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        log.info("Starting IoT sensor simulator, publishing to MQTT topic '{}/{{deviceId}}/data' every {}s",
                topicPrefix, intervalSeconds);
        scheduler.scheduleAtFixedRate(this::emitRandomReading, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    private void emitRandomReading() {
        try {
            String deviceId = DEVICE_IDS.get(ThreadLocalRandom.current().nextInt(DEVICE_IDS.size()));
            boolean simulateAnomaly = deviceId.equals("server-room-01") && ThreadLocalRandom.current().nextInt(10) == 0;

            double temperature = simulateAnomaly
                    ? ThreadLocalRandom.current().nextDouble(31, 40)
                    : ThreadLocalRandom.current().nextDouble(18, 28);
            double humidity = ThreadLocalRandom.current().nextDouble(30, 70);

            SensorDataDto reading = SensorDataDto.builder()
                    .deviceId(deviceId)
                    .temperature(Math.round(temperature * 10.0) / 10.0)
                    .humidity(Math.round(humidity * 10.0) / 10.0)
                    .build();

            MqttMessage message = new MqttMessage(objectMapper.writeValueAsBytes(reading));
            message.setQos(qos);
            mqttClient.publish(topicPrefix + "/" + deviceId + "/data", message);

            log.info("Simulator published reading for {}: temp={}°C humidity={}%",
                    deviceId, reading.getTemperature(), reading.getHumidity());
        } catch (Exception e) {
            log.error("Simulator failed to publish reading", e);
        }
    }
}
