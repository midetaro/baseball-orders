package com.example.baseballorders.backend;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.application.UserAccountRepository;
import com.example.baseballorders.backend.application.UserAccountService;
import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** application層のユースケースをSpringへ登録する構成。 */
@Configuration
public class InfrastructureConfiguration {
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
     * シミュレーションCoordinatorを生成する。
     *
     * @param publisher シミュレーション要求送信ポート
     * @param registry 結果相関レジストリ
     * @return 構成済みCoordinator
     */
    @Bean
    public SimulationCoordinator simulationCoordinator(
            SimulatorMessagePublisher publisher, WaitingResultRegistry registry) {
        return new SimulationCoordinator(publisher, registry);
    }
}
