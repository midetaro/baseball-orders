package com.example.baseballorders.backend;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.application.SimulationLimits;
import com.example.baseballorders.backend.application.SimulationLimitsBuilder;
import com.example.baseballorders.backend.application.UserAccountRepository;
import com.example.baseballorders.backend.application.UserAccountService;
import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import java.time.Clock;
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
     * @param maximumAverageSluggish 打順9人の長打率平均の上限
     * @param pitcherIncreaseMultiplier 投手性格による打撃確率の増加倍率
     * @param maximumSuccessRate バント成功率と盗塁成功率の受付上限
     */
    public InfrastructureConfiguration(
            @Value("${baseball-orders.simulation.result-timeout}") Duration resultTimeout,
            @Value("${baseball-orders.simulation.maximum-average-hit-average}")
                    float maximumAverageHitAverage,
            @Value("${baseball-orders.simulation.maximum-average-sluggish}")
                    float maximumAverageSluggish,
            @Value("${baseball-orders.simulation.pitcher-increase-multiplier}")
                    float pitcherIncreaseMultiplier,
            @Value("${baseball-orders.simulation.maximum-success-rate}") float maximumSuccessRate) {
        simulationLimits =
                SimulationLimitsBuilder.simulationLimits()
                        .resultTimeout(resultTimeout)
                        .maximumAverageHitAverage(maximumAverageHitAverage)
                        .maximumAverageSluggish(maximumAverageSluggish)
                        .pitcherIncreaseMultiplier(pitcherIncreaseMultiplier)
                        .maximumSuccessRate(maximumSuccessRate)
                        .build();
    }

    /**
     * UTCの現在時刻を提供する。
     *
     * @return UTCクロック
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Google OIDCアカウントのユースケースを生成する。
     *
     * @param repository アカウント永続化ポート
     * @return 構成済みアカウントサービス
     */
    @Bean
    public UserAccountService userAccountService(UserAccountRepository repository) {
        return new UserAccountService(repository);
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
