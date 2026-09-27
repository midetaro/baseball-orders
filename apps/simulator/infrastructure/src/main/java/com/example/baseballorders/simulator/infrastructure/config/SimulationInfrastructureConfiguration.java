package com.example.baseballorders.simulator.infrastructure.config;

import com.example.baseballorders.simulator.domain.game.BaseStateFactory;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StealStrategy;
import com.example.baseballorders.simulator.domain.rule.SimulationRules;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Configures infrastructure and domain collaborators required to consume simulation requests. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({SimulationRuleProperties.class, SimulationPitcherProperties.class})
@RequiredArgsConstructor
public class SimulationInfrastructureConfiguration {

    /** {@code simulation.rule} 配下の束縛済み確率設定。 */
    private final SimulationRuleProperties ruleProperties;

    /**
     * 設定ファイルの確率値をドメインの確率設定へ変換する。
     *
     * @return シミュレーションの確率設定
     */
    @Bean
    public SimulationRules simulationRules() {
        return ruleProperties.toSimulationRules();
    }

    /**
     * 設定された確率を保持する行動戦略ファクトリを生成する。
     *
     * @return 行動戦略ファクトリ
     */
    @Bean
    public BehaviorStrategies behaviorStrategies() {
        return new BehaviorStrategies(simulationRules());
    }

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
        return new BaseStateFactory(simulationRules().runnerAdvance());
    }

    /**
     * Creates the default hitting strategy assigned to simulation players.
     *
     * @return middle-distance hitting strategy
     */
    @Bean
    public HittingStrategy hittingStrategy() {
        return behaviorStrategies().middleDistanceHittingStrategy();
    }

    /**
     * Creates the default stealing strategy assigned to simulation players.
     *
     * @return standard stealing strategy
     */
    @Bean
    public StealStrategy stealStrategy() {
        return behaviorStrategies().standardSteal();
    }

    /**
     * Creates the default bunt strategy assigned to simulation players.
     *
     * @return standard bunt strategy
     */
    @Bean
    public BuntStrategy buntStrategy() {
        return behaviorStrategies().standardBunt();
    }
}
