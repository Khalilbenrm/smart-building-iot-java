package com.smariot.kafka.producer;

import com.smariot.config.KafkaTopicConfig;
import com.smariot.model.dto.SensorDataDto;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorDataProducerTest {

    @Mock
    private KafkaTemplate<String, SensorDataDto> kafkaTemplate;

    @Test
    void sendSensorData_shouldPublishWithDeviceIdAsKey() {
        SensorDataProducer producer = new SensorDataProducer(kafkaTemplate);
        SensorDataDto dto = SensorDataDto.builder()
                .deviceId("hvac-critical-01")
                .temperature(22.0)
                .humidity(50.0)
                .build();

        ProducerRecord<String, SensorDataDto> producerRecord =
                new ProducerRecord<>(KafkaTopicConfig.TOPIC_NAME, dto.getDeviceId(), dto);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(KafkaTopicConfig.TOPIC_NAME, 0), 0, 0, 0, 0, 0);
        SendResult<String, SensorDataDto> sendResult = new SendResult<>(producerRecord, metadata);

        when(kafkaTemplate.send(KafkaTopicConfig.TOPIC_NAME, dto.getDeviceId(), dto))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        producer.sendSensorData(dto);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SensorDataDto> valueCaptor = ArgumentCaptor.forClass(SensorDataDto.class);
        verify(kafkaTemplate).send(any(String.class), keyCaptor.capture(), valueCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("hvac-critical-01");
        assertThat(valueCaptor.getValue()).isEqualTo(dto);
    }

    @Test
    void sendSensorData_shouldNotThrowWhenKafkaSendFails() {
        SensorDataProducer producer = new SensorDataProducer(kafkaTemplate);
        SensorDataDto dto = SensorDataDto.builder()
                .deviceId("corridor-01")
                .temperature(20.0)
                .humidity(40.0)
                .build();

        CompletableFuture<SendResult<String, SensorDataDto>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("broker unavailable"));

        when(kafkaTemplate.send(KafkaTopicConfig.TOPIC_NAME, dto.getDeviceId(), dto)).thenReturn(failedFuture);

        producer.sendSensorData(dto);

        verify(kafkaTemplate).send(KafkaTopicConfig.TOPIC_NAME, dto.getDeviceId(), dto);
    }
}
