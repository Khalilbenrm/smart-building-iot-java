package com.smariot.service;

import com.smariot.exception.InvalidSensorDataException;
import com.smariot.kafka.producer.SensorDataProducer;
import com.smariot.model.dto.SensorDataDto;
import com.smariot.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Sensor data orchestration: validates business rules, timestamps the reading,
 * and delegates persistence to Kafka (write path) / MongoDB (read path).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataServiceImpl implements SensorDataService {

    private static final double MIN_REALISTIC_TEMPERATURE = -80.0;
    private static final double MAX_REALISTIC_TEMPERATURE = 150.0;
    private static final double MIN_HUMIDITY = 0.0;
    private static final double MAX_HUMIDITY = 100.0;

    private final SensorDataProducer sensorDataProducer;
    private final SensorDataRepository sensorDataRepository;

    @Override
    public SensorDataDto ingestSensorData(SensorDataDto dto) {
        validate(dto);

        SensorDataDto enriched = dto.toBuilder()
                .timestamp(Instant.now())
                .build();

        sensorDataProducer.sendSensorData(enriched);
        return enriched;
    }

    @Override
    public List<SensorDataDto> getByDeviceId(String deviceId) {
        return sensorDataRepository.findByDeviceId(deviceId).stream()
                .map(SensorDataDto::fromEntity)
                .toList();
    }

    @Override
    public List<SensorDataDto> getByTimeRange(Instant start, Instant end) {
        return sensorDataRepository.findByTimestampBetween(start, end).stream()
                .map(SensorDataDto::fromEntity)
                .toList();
    }

    private void validate(SensorDataDto dto) {
        if (dto.getTemperature() < MIN_REALISTIC_TEMPERATURE || dto.getTemperature() > MAX_REALISTIC_TEMPERATURE) {
            log.error("Invalid temperature value for device {}: {}", dto.getDeviceId(), dto.getTemperature());
            throw new InvalidSensorDataException("Temperature out of realistic range: " + dto.getTemperature());
        }
        if (dto.getHumidity() < MIN_HUMIDITY || dto.getHumidity() > MAX_HUMIDITY) {
            log.error("Invalid humidity value for device {}: {}", dto.getDeviceId(), dto.getHumidity());
            throw new InvalidSensorDataException("Humidity out of range: " + dto.getHumidity());
        }
    }
}
