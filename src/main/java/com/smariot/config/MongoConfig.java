package com.smariot.config;

import com.smariot.model.entity.SensorDataEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration: enables repository scanning and guarantees the
 * deviceId / timestamp indexes required by the historical and range queries.
 */
@Slf4j
@Configuration
@EnableMongoRepositories(basePackages = "com.smariot.repository")
public class MongoConfig {

    @Bean
    public CommandLineRunner ensureSensorDataIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            IndexOperations indexOps = mongoTemplate.indexOps(SensorDataEntity.class);
            indexOps.ensureIndex(new Index().on("deviceId", org.springframework.data.domain.Sort.Direction.ASC));
            indexOps.ensureIndex(new Index().on("timestamp", org.springframework.data.domain.Sort.Direction.ASC));
            log.info("Ensured MongoDB indexes on sensor_data (deviceId, timestamp)");
        };
    }
}
