package com.smariot.kafka.consumer;

import com.smariot.model.dto.SensorDataDto;
import com.smariot.model.entity.SensorDataEntity;
import com.smariot.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes sensor readings from the iot-sensor-data topic, applies simple
 * anomaly detection, and persists valid readings to MongoDB.
 * Invalid records are logged and discarded rather than blocking the consumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDataConsumer {

    private static final double HIGH_TEMPERATURE_THRESHOLD = 30.0;
    private static final double HIGH_HUMIDITY_THRESHOLD = 80.0;

    private final SensorDataRepository sensorDataRepository;

    @KafkaListener(
            topics = "iot-sensor-data",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "sensorDataKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, SensorDataDto> record) {
        SensorDataDto dto = record.value();
        log.info("Consumed sensor record key={} partition={} offset={}", record.key(), record.partition(), record.offset());

        if (!isValid(dto)) {
            log.error("Invalid sensor data received, discarding message: {}", dto);
            return;
        }

        detectAnomalies(dto);

        SensorDataEntity entity = toEntity(dto);
        sensorDataRepository.save(entity);
        log.info("Persisted sensor data for device {} at {}", entity.getDeviceId(), entity.getTimestamp());
    }

    private boolean isValid(SensorDataDto dto) {
        return dto != null
                && dto.getDeviceId() != null && !dto.getDeviceId().isBlank()
                && dto.getTemperature() != null
                && dto.getHumidity() != null;
    }

    private void detectAnomalies(SensorDataDto dto) {
        if (dto.getTemperature() > HIGH_TEMPERATURE_THRESHOLD) {
            log.warn("High temperature detected for device {}: {}°C", dto.getDeviceId(), dto.getTemperature());
        }
        if (dto.getHumidity() > HIGH_HUMIDITY_THRESHOLD) {
            log.warn("High humidity detected for device {}: {}%", dto.getDeviceId(), dto.getHumidity());
        }
    }

    private SensorDataEntity toEntity(SensorDataDto dto) {
        return SensorDataEntity.builder()
                .deviceId(dto.getDeviceId())
                .temperature(dto.getTemperature())
                .humidity(dto.getHumidity())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now())
                .build();
    }
}
