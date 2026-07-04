package com.smariot.service;

import com.smariot.model.dto.SensorDataDto;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrates ingestion (validation + enrichment + dispatch to Kafka) and
 * historical queries of sensor data.
 */
public interface SensorDataService {

    SensorDataDto ingestSensorData(SensorDataDto dto);

    List<SensorDataDto> getByDeviceId(String deviceId);

    List<SensorDataDto> getByTimeRange(Instant start, Instant end);
}
