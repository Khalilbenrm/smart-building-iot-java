package com.smariot.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB persistence model for a sensor reading.
 * Indexed on deviceId and timestamp to support the historical/range queries.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sensor_data")
public class SensorDataEntity {

    @Id
    private String id;

    @Indexed
    private String deviceId;

    private Double temperature;

    private Double humidity;

    @Indexed
    private Instant timestamp;
}
