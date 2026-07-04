package com.smariot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI documentation metadata, exposed at /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartIotOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Building IoT API")
                        .description("Real-Time Event-Driven Backend for a Smart IoT Building: "
                                + "ingestion, processing and historical query of sensor telemetry.")
                        .version("1.0.0")
                        .contact(new Contact().name("Smart IoT Building")));
    }
}
