package com.smariot.repository;

import com.smariot.model.entity.SensorDataEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB access layer for sensor readings. No business logic here.
 */
public interface SensorDataRepository extends MongoRepository<SensorDataEntity, String> {

    List<SensorDataEntity> findByDeviceId(String deviceId);

    List<SensorDataEntity> findByTimestampBetween(Instant start, Instant end);
}
