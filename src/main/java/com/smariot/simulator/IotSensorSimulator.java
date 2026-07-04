package com.smariot.simulator;

import com.smariot.model.dto.SensorDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Bonus: continuously generates realistic sensor readings for a handful of
 * building zones and posts them to the ingestion endpoint. Enabled only under
 * the "simulator" profile so it never runs as part of normal application startup.
 *
 * Usage: java -jar smart-iot-building.jar --spring.profiles.active=simulator
 */
@Slf4j
@Component
@Profile("simulator")
public class IotSensorSimulator implements CommandLineRunner {

    private static final List<String> DEVICE_IDS = List.of(
            "office-01", "meeting-room-01", "corridor-01", "server-room-01", "hvac-critical-01");

    private final RestTemplate restTemplate = new RestTemplate();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${simulator.target-url:http://localhost:8080/api/sensors/data}")
    private String targetUrl;

    @Value("${simulator.interval-seconds:3}")
    private long intervalSeconds;

    @Override
    public void run(String... args) {
        log.info("Starting IoT sensor simulator, posting to {} every {}s", targetUrl, intervalSeconds);
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

            restTemplate.postForEntity(targetUrl, reading, SensorDataDto.class);
            log.info("Simulator sent reading for {}: temp={}°C humidity={}%",
                    deviceId, reading.getTemperature(), reading.getHumidity());
        } catch (Exception e) {
            log.error("Simulator failed to send reading", e);
        }
    }
}
