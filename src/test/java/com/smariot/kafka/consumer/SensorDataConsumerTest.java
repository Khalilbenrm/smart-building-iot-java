package com.smariot.kafka.consumer;

import com.smariot.model.dto.SensorDataDto;
import com.smariot.model.entity.SensorDataEntity;
import com.smariot.repository.SensorDataRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorDataConsumerTest {

    @Mock
    private SensorDataRepository sensorDataRepository;

    @Test
    void consume_shouldPersistValidReading() {
        SensorDataConsumer consumer = new SensorDataConsumer(sensorDataRepository);
        SensorDataDto dto = SensorDataDto.builder()
                .deviceId("office-01")
                .temperature(22.5)
                .humidity(48.0)
                .timestamp(Instant.now())
                .build();
        ConsumerRecord<String, SensorDataDto> record =
                new ConsumerRecord<>("iot-sensor-data", 0, 0L, dto.getDeviceId(), dto);

        consumer.consume(record);

        ArgumentCaptor<SensorDataEntity> captor = ArgumentCaptor.forClass(SensorDataEntity.class);
        verify(sensorDataRepository).save(captor.capture());
        assertThat(captor.getValue().getDeviceId()).isEqualTo("office-01");
        assertThat(captor.getValue().getTemperature()).isEqualTo(22.5);
    }

    @Test
    void consume_shouldDiscardRecordWithMissingDeviceId() {
        SensorDataConsumer consumer = new SensorDataConsumer(sensorDataRepository);
        SensorDataDto dto = SensorDataDto.builder()
                .deviceId(" ")
                .temperature(22.5)
                .humidity(48.0)
                .build();
        ConsumerRecord<String, SensorDataDto> record =
                new ConsumerRecord<>("iot-sensor-data", 0, 0L, "key", dto);

        consumer.consume(record);

        verify(sensorDataRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void consume_shouldPersistDespiteHighTemperatureAnomaly() {
        SensorDataConsumer consumer = new SensorDataConsumer(sensorDataRepository);
        SensorDataDto dto = SensorDataDto.builder()
                .deviceId("server-room-01")
                .temperature(35.0)
                .humidity(85.0)
                .timestamp(Instant.now())
                .build();
        ConsumerRecord<String, SensorDataDto> record =
                new ConsumerRecord<>("iot-sensor-data", 0, 0L, dto.getDeviceId(), dto);

        consumer.consume(record);

        verify(sensorDataRepository).save(org.mockito.ArgumentMatchers.any(SensorDataEntity.class));
    }
}
