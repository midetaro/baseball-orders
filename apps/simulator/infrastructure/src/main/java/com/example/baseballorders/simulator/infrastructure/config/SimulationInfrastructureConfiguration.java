package com.example.baseballorders.simulator.infrastructure.config;

import com.example.baseballorders.simulator.domain.game.BaseStateFactory;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StealStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Configures infrastructure and domain collaborators required to consume simulation requests. */
@Configuration
@EnableScheduling
public class SimulationInfrastructureConfiguration {

    /**
     * Creates the JSON object mapper used for SQS request bodies.
     *
     * @return SQS message object mapper
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

    /**
     * Creates the stateless factory used to construct game-specific base states.
     *
     * @return base-state factory
     */
    @Bean
    public BaseStateFactory baseStateFactory() {
        return new BaseStateFactory();
    }

    /**
     * Creates the default hitting strategy assigned to simulation players.
     *
     * @return middle-distance hitting strategy
     */
    @Bean
    public HittingStrategy hittingStrategy() {
        return BehaviorStrategies.middleDistanceHittingStrategy();
    }

    /**
     * Creates the default stealing strategy assigned to simulation players.
     *
     * @return standard stealing strategy
     */
    @Bean
    public StealStrategy stealStrategy() {
        return BehaviorStrategies.standardSteal();
    }

    /**
     * Creates the default bunt strategy assigned to simulation players.
     *
     * @return standard bunt strategy
     */
    @Bean
    public BuntStrategy buntStrategy() {
        return BehaviorStrategies.standardBunt();
    }
}
