package com.smariot.controller;

import com.smariot.model.dto.SensorDataDto;
import com.smariot.service.SensorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * REST entry points for IoT sensor telemetry. Pure HTTP mapping only —
 * all business logic lives in SensorDataService.
 */
@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
@Tag(name = "Sensor Data", description = "Ingestion and querying of IoT sensor telemetry")
public class SensorDataController {

    private final SensorDataService sensorDataService;

    @Operation(summary = "Ingest a sensor reading", description = "Publishes the reading to Kafka for asynchronous processing")
    @PostMapping("/data")
    public ResponseEntity<SensorDataDto> ingest(@Valid @RequestBody SensorDataDto dto) {
        SensorDataDto accepted = sensorDataService.ingestSensorData(dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(accepted);
    }

    @Operation(summary = "Get historical readings for a device")
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<SensorDataDto>> getByDevice(@PathVariable String deviceId) {
        return ResponseEntity.ok(sensorDataService.getByDeviceId(deviceId));
    }

    @Operation(summary = "Get readings within a time range")
    @GetMapping("/range")
    public ResponseEntity<List<SensorDataDto>> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        return ResponseEntity.ok(sensorDataService.getByTimeRange(start, end));
    }
}
