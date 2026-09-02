package com.example.baseballorders.backend;

import com.example.baseballorders.backend.simulation.application.SimulationCoordinator;
import com.example.baseballorders.backend.simulation.application.SimulatorMessagePublisher;
import com.example.baseballorders.backend.simulation.application.WaitingResultRegistry;
import com.example.baseballorders.backend.simulation.application.PlayerDataRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** application層のユースケースをSpringへ登録する構成。 */
@Configuration
public class InfrastructureConfiguration {

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
     * シミュレーションCoordinatorを生成する。
     *
     * @param repository 選手データ取得ポート
     * @param publisher シミュレーション要求送信ポート
     * @param registry 結果相関レジストリ
     * @return 構成済みCoordinator
     */
    @Bean
    public SimulationCoordinator simulationCoordinator(
            PlayerDataRepository repository,
            SimulatorMessagePublisher publisher,
            WaitingResultRegistry registry) {
        return new SimulationCoordinator(repository, publisher, registry);
    }
}
