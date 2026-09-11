package com.example.baseballorders.backend;

import com.example.baseballorders.backend.application.PasswordHasher;
import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.application.UserAccountRepository;
import com.example.baseballorders.backend.application.UserAccountService;
import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.application.adapter.PlayerDataRepository;
import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** application層のユースケースをSpringへ登録する構成。 */
@Configuration
public class InfrastructureConfiguration {

    /**
     * BCryptを使用するパスワードハッシュ化ポートを生成する。
     *
     * @return BCryptパスワードハッシュ化ポート
     */
    @Bean
    public PasswordHasher passwordHasher() {
        var encoder = new BCryptPasswordEncoder();
        return new PasswordHasher() {
            @Override
            public String hash(String password) {
                return encoder.encode(password);
            }

            @Override
            public boolean matches(String password, String passwordHash) {
                return encoder.matches(password, passwordHash);
            }
        };
    }

    /**
     * アカウント管理ユースケースを生成する。
     *
     * @param repository アカウント永続化ポート
     * @param passwordHasher パスワードハッシュ化ポート
     * @return アカウント管理ユースケース
     */
    @Bean
    public UserAccountService userAccountService(
            UserAccountRepository repository, PasswordHasher passwordHasher) {
        return new UserAccountService(repository, passwordHasher);
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
