package com.smariot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Smart Building IoT - Real-Time Event-Driven Backend System.
 * Flow: Sensors (MQTT) -> MQTT Subscriber -> Service -> Kafka Producer -> Kafka Topic -> Kafka Consumer -> MongoDB
 */
@SpringBootApplication
public class SmartIotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartIotApplication.class, args);
    }
}
