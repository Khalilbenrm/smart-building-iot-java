package com.smariot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smariot.model.dto.SensorDataDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer wiring for the iot-sensor-data topic.
 * Deserialization/processing failures are retried a bounded number of times, then logged
 * and skipped via DefaultErrorHandler so a single bad record never blocks the consumer.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, SensorDataDto> consumerFactory(@Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.smariot.model.dto");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SensorDataDto.class.getName());
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, SensorDataDto> factory =
                new DefaultKafkaConsumerFactory<>(configProps);
        factory.setValueDeserializer(new ErrorHandlingDeserializer<>(new JsonDeserializer<>(SensorDataDto.class, kafkaObjectMapper)));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SensorDataDto> sensorDataKafkaListenerContainerFactory(
            ConsumerFactory<String, SensorDataDto> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, SensorDataDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Retry a failing record 3 times with a 1s backoff, then log and move on without blocking the consumer.
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error("Error consuming record key={} partition={} offset={}: {}",
                        record.key(), record.partition(), record.offset(), exception.getMessage(), exception),
                new FixedBackOff(1000L, 3L));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
