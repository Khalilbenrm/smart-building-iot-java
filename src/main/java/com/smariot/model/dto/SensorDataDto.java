package com.smariot.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smariot.model.entity.SensorDataEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Data Transfer Object representing a single IoT sensor reading.
 * Used both as the REST ingestion payload and as the read-model returned by query endpoints.
 */
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "IoT sensor reading payload")
public class SensorDataDto {

    @NotBlank(message = "deviceId must not be blank")
    @Schema(description = "Unique identifier of the sensor/device", example = "server-room-01")
    private String deviceId;

    @NotNull(message = "temperature is required")
    @Schema(description = "Temperature in Celsius", example = "24.5")
    private Double temperature;

    @NotNull(message = "humidity is required")
    @Schema(description = "Relative humidity percentage", example = "45.0")
    private Double humidity;

    @Schema(description = "Timestamp of the reading (assigned server-side if omitted)")
    private Instant timestamp;

    public static SensorDataDto fromEntity(SensorDataEntity entity) {
        return SensorDataDto.builder()
                .deviceId(entity.getDeviceId())
                .temperature(entity.getTemperature())
                .humidity(entity.getHumidity())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
