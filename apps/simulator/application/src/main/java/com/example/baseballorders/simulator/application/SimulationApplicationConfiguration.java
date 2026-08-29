package com.example.baseballorders.simulator.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Configures the application services required to consume simulation requests. */
@Configuration
@EnableScheduling
public class SimulationApplicationConfiguration {

    /**
     * Creates the JSON object mapper used for SQS request bodies.
     *
     * @return application object mapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Creates the AWS SQS client using the standard AWS configuration chain.
     *
     * @return configured SQS client
     */
    @Bean(destroyMethod = "close")
    public SqsClient sqsClient() {
        return SqsClient.create();
    }
}
