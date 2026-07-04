package com.smariot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dedicated ObjectMapper for Kafka JSON (de)serialization, registered separately from the
 * Spring MVC ObjectMapper so both can be autowired unambiguously via @Qualifier.
 */
@Configuration
public class KafkaObjectMapperConfig {

    @Bean("kafkaObjectMapper")
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
