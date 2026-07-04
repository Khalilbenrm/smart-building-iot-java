package com.smariot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smariot.model.dto.SensorDataDto;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer wiring: String key (deviceId) / JSON value (SensorDataDto), with
 * delivery guarantees suited to telemetry ingestion (acks=all, bounded retries).
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, SensorDataDto> producerFactory(@Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(),
                new JsonSerializer<>(kafkaObjectMapper));
    }

    @Bean
    public KafkaTemplate<String, SensorDataDto> kafkaTemplate(ProducerFactory<String, SensorDataDto> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
