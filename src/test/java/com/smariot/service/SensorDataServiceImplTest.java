package com.smariot.service;

import com.smariot.exception.InvalidSensorDataException;
import com.smariot.kafka.producer.SensorDataProducer;
import com.smariot.model.dto.SensorDataDto;
import com.smariot.model.entity.SensorDataEntity;
import com.smariot.repository.SensorDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorDataServiceImplTest {

    @Mock
    private SensorDataProducer sensorDataProducer;

    @Mock
    private SensorDataRepository sensorDataRepository;

    private SensorDataService sensorDataService;

    @BeforeEach
    void setUp() {
        sensorDataService = new SensorDataServiceImpl(sensorDataProducer, sensorDataRepository);
    }

    @Test
    void ingestSensorData_shouldEnrichTimestampAndDispatchToProducer() {
        SensorDataDto input = SensorDataDto.builder()
                .deviceId("server-room-01")
                .temperature(24.5)
                .humidity(45.0)
                .build();

        SensorDataDto result = sensorDataService.ingestSensorData(input);

        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo("server-room-01");

        ArgumentCaptor<SensorDataDto> captor = ArgumentCaptor.forClass(SensorDataDto.class);
        verify(sensorDataProducer).sendSensorData(captor.capture());
        assertThat(captor.getValue().getTimestamp()).isEqualTo(result.getTimestamp());
    }

    @Test
    void ingestSensorData_shouldRejectUnrealisticTemperature() {
        SensorDataDto input = SensorDataDto.builder()
                .deviceId("server-room-01")
                .temperature(999.0)
                .humidity(45.0)
                .build();

        assertThatThrownBy(() -> sensorDataService.ingestSensorData(input))
                .isInstanceOf(InvalidSensorDataException.class);

        verifyNoInteractions(sensorDataProducer);
    }

    @Test
    void ingestSensorData_shouldRejectInvalidHumidity() {
        SensorDataDto input = SensorDataDto.builder()
                .deviceId("server-room-01")
                .temperature(22.0)
                .humidity(150.0)
                .build();

        assertThatThrownBy(() -> sensorDataService.ingestSensorData(input))
                .isInstanceOf(InvalidSensorDataException.class);

        verifyNoInteractions(sensorDataProducer);
    }

    @Test
    void getByDeviceId_shouldMapEntitiesToDtos() {
        Instant now = Instant.now();
        SensorDataEntity entity = SensorDataEntity.builder()
                .id("1")
                .deviceId("office-01")
                .temperature(21.0)
                .humidity(40.0)
                .timestamp(now)
                .build();
        when(sensorDataRepository.findByDeviceId("office-01")).thenReturn(List.of(entity));

        List<SensorDataDto> result = sensorDataService.getByDeviceId("office-01");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceId()).isEqualTo("office-01");
        assertThat(result.get(0).getTimestamp()).isEqualTo(now);
    }

    @Test
    void getByTimeRange_shouldDelegateToRepository() {
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        when(sensorDataRepository.findByTimestampBetween(any(), any())).thenReturn(List.of());

        List<SensorDataDto> result = sensorDataService.getByTimeRange(start, end);

        assertThat(result).isEmpty();
        verify(sensorDataRepository).findByTimestampBetween(start, end);
    }
}
