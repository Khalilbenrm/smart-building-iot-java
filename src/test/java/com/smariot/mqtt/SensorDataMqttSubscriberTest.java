package com.smariot.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smariot.model.dto.SensorDataDto;
import com.smariot.service.SensorDataService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorDataMqttSubscriberTest {

    @Mock
    private MqttClient mqttClient;

    @Mock
    private SensorDataService sensorDataService;

    private SensorDataMqttSubscriber subscriber;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        subscriber = new SensorDataMqttSubscriber(mqttClient, sensorDataService, objectMapper);
        ReflectionTestUtils.setField(subscriber, "topicFilter", "iot/sensors/+/data");
        ReflectionTestUtils.setField(subscriber, "qos", 1);
    }

    @Test
    void init_shouldRegisterCallbackAndSubscribeToTopicFilter() throws Exception {
        subscriber.init();

        verify(mqttClient).setCallback(subscriber);
        verify(mqttClient).subscribe("iot/sensors/+/data", 1);
    }

    @Test
    void messageArrived_shouldIngestValidReading() throws Exception {
        SensorDataDto dto = SensorDataDto.builder()
                .deviceId("office-01")
                .temperature(22.5)
                .humidity(48.0)
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        MqttMessage message = new MqttMessage(objectMapper.writeValueAsBytes(dto));

        subscriber.messageArrived("iot/sensors/office-01/data", message);

        ArgumentCaptor<SensorDataDto> captor = ArgumentCaptor.forClass(SensorDataDto.class);
        verify(sensorDataService).ingestSensorData(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getDeviceId()).isEqualTo("office-01");
    }

    @Test
    void messageArrived_shouldDiscardUnparsablePayloadWithoutThrowing() {
        MqttMessage message = new MqttMessage("not-json".getBytes());

        subscriber.messageArrived("iot/sensors/office-01/data", message);

        verify(sensorDataService, never()).ingestSensorData(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void connectComplete_shouldResubscribeOnReconnect() throws Exception {
        subscriber.connectComplete(true, "tcp://localhost:1883");

        verify(mqttClient).subscribe(anyString(), anyInt());
    }
}
