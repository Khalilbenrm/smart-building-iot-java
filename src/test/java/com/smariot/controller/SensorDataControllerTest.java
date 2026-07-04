package com.smariot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smariot.exception.GlobalExceptionHandler;
import com.smariot.model.dto.SensorDataDto;
import com.smariot.service.SensorDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorDataController.class)
@org.springframework.context.annotation.Import(GlobalExceptionHandler.class)
class SensorDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SensorDataService sensorDataService;

    @Test
    void ingest_shouldReturn202WithEnrichedPayload() throws Exception {
        Instant timestamp = Instant.parse("2026-07-04T10:00:00Z");
        SensorDataDto request = SensorDataDto.builder()
                .deviceId("server-room-01")
                .temperature(24.5)
                .humidity(45.0)
                .build();
        SensorDataDto response = request.toBuilder().timestamp(timestamp).build();

        when(sensorDataService.ingestSensorData(any(SensorDataDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/sensors/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.deviceId").value("server-room-01"))
                .andExpect(jsonPath("$.temperature").value(24.5));
    }

    @Test
    void ingest_shouldReturn400WhenDeviceIdMissing() throws Exception {
        SensorDataDto invalid = SensorDataDto.builder()
                .temperature(24.5)
                .humidity(45.0)
                .build();

        mockMvc.perform(post("/api/sensors/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void getByDevice_shouldReturnHistoricalReadings() throws Exception {
        SensorDataDto reading = SensorDataDto.builder()
                .deviceId("office-01")
                .temperature(21.0)
                .humidity(40.0)
                .timestamp(Instant.now())
                .build();
        when(sensorDataService.getByDeviceId("office-01")).thenReturn(List.of(reading));

        mockMvc.perform(get("/api/sensors/device/office-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value("office-01"));
    }

    @Test
    void getByRange_shouldDelegateToService() throws Exception {
        when(sensorDataService.getByTimeRange(any(Instant.class), any(Instant.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/sensors/range")
                        .param("start", "2026-07-01T00:00:00Z")
                        .param("end", "2026-07-04T00:00:00Z"))
                .andExpect(status().isOk());

        verify(sensorDataService).getByTimeRange(eq(Instant.parse("2026-07-01T00:00:00Z")),
                eq(Instant.parse("2026-07-04T00:00:00Z")));
    }
}
