package com.example.baseballorders.backend;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.application.SimulationLimits;
import com.example.baseballorders.backend.application.SimulationLimitsBuilder;
import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** application層のユースケースをSpringへ登録する構成。 */
@Configuration
public class InfrastructureConfiguration {

    private final SimulationLimits simulationLimits;

    /**
     * 設定ファイルのシミュレーション上限値を受け取って構成を作成する。
     *
     * @param resultTimeout SQS結果を待機する時間
     * @param maximumAverageHitAverage 打順9人の打率平均の上限
     */
    public InfrastructureConfiguration(
            @Value("${baseball-orders.simulation.result-timeout}") Duration resultTimeout,
            @Value("${baseball-orders.simulation.maximum-average-hit-average}")
                    float maximumAverageHitAverage) {
        simulationLimits =
                SimulationLimitsBuilder.simulationLimits()
                        .resultTimeout(resultTimeout)
                        .maximumAverageHitAverage(maximumAverageHitAverage)
                        .build();
    }

    /**
     * 待機結果レジストリを生成する。
     *
     * @return 新しい待機結果レジストリ
     */
    @Bean
    public WaitingResultRegistry waitingResultRegistry() {
        return new WaitingResultRegistry();
    }

    /**
     * シミュレーションCoordinatorを設定値の上限とともに生成する。
     *
     * @param publisher シミュレーション要求送信ポート
     * @param registry 結果相関レジストリ
     * @return 構成済みCoordinator
     */
    @Bean
    public SimulationCoordinator simulationCoordinator(
            SimulatorMessagePublisher publisher, WaitingResultRegistry registry) {
        return new SimulationCoordinator(publisher, registry, simulationLimits);
    }
}
