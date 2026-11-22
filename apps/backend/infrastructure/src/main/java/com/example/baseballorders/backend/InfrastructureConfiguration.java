package com.example.baseballorders.backend;

import com.example.baseballorders.backend.application.DefaultLineupQuery;
import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.application.SimulationLimits;
import com.example.baseballorders.backend.application.SimulationLimitsBuilder;
import com.example.baseballorders.backend.application.adapter.DefaultLineupRepository;
import com.example.baseballorders.backend.application.adapter.SimulatorClient;
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
     * @param maximumAverageHitAverage 打順9人の打率平均の上限
     */
    public InfrastructureConfiguration(
            @Value("${baseball-orders.simulation.maximum-average-hit-average}")
                    float maximumAverageHitAverage) {
        simulationLimits =
                SimulationLimitsBuilder.simulationLimits()
                        .maximumAverageHitAverage(maximumAverageHitAverage)
                        .build();
    }

    /**
     * シミュレーションCoordinatorを設定値の上限とともに生成する。
     *
     * @param simulatorClient simulatorへ要求を送り結果を同期的に受け取るポートの実装
     * @return 構成済みCoordinator
     */
    @Bean
    public SimulationCoordinator simulationCoordinator(SimulatorClient simulatorClient) {
        return new SimulationCoordinator(simulatorClient, simulationLimits);
    }

    /**
     * チーム別の既定オーダー取得ユースケースを生成する。
     *
     * @param repository 既定オーダー取得ポートの実装
     * @return 構成済みユースケース
     */
    @Bean
    public DefaultLineupQuery defaultLineupQuery(DefaultLineupRepository repository) {
        return new DefaultLineupQuery(repository);
    }
}
