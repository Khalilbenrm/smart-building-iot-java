package com.smariot.kafka.producer;

import com.smariot.config.KafkaTopicConfig;
import com.smariot.model.dto.SensorDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes sensor readings to the iot-sensor-data topic, keyed by deviceId
 * so all readings for a given device stay ordered on the same partition.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDataProducer {

    private final KafkaTemplate<String, SensorDataDto> kafkaTemplate;

    public void sendSensorData(SensorDataDto dto) {
        log.info("Publishing sensor data to topic '{}' for device {}", KafkaTopicConfig.TOPIC_NAME, dto.getDeviceId());

        kafkaTemplate.send(KafkaTopicConfig.TOPIC_NAME, dto.getDeviceId(), dto)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish sensor data for device {}", dto.getDeviceId(), exception);
                    } else {
                        log.info("Published sensor data for device {} -> partition {} offset {}",
                                dto.getDeviceId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
