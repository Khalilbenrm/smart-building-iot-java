package com.smariot.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the iot-sensor-data topic so it is auto-provisioned on broker startup.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_NAME = "iot-sensor-data";

    @Bean
    public NewTopic iotSensorDataTopic() {
        return TopicBuilder.name(TOPIC_NAME)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
