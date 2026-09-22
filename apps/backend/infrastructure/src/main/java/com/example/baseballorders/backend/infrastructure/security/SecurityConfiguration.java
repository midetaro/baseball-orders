package com.example.baseballorders.backend.infrastructure.security;

import com.example.baseballorders.backend.application.UserAccountService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/** Google OAuth設定の有無に応じてSpring Securityを構成する。 */
@Configuration
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class SecurityConfiguration {
    /**
     * 認証済みGoogleユーザーを永続化するサービスを生成する。
     *
     * @param userAccountService アカウント作成ユースケース
     * @return OIDCユーザーサービス
     */
    @Bean
    public GoogleOidcUserProvisioningService googleOidcUserProvisioningService(
            UserAccountService userAccountService) {
        return new GoogleOidcUserProvisioningService(userAccountService);
    }

    /**
     * Google OAuthが設定済みの場合だけクライアント登録を公開する。
     *
     * @param properties Google OAuth設定
     * @return Googleのクライアント登録。未設定時はnull
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            GoogleOAuthProperties properties) {
        if (!properties.enabled()) {
            return registrationId -> null;
        }
        ClientRegistration registration =
                ClientRegistrations.fromIssuerLocation("https://accounts.google.com")
                        .registrationId("google")
                        .clientId(properties.clientId())
                        .clientSecret(properties.clientSecret())
                        .scope("openid", "profile", "email")
                        .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    /**
     * 全ページを匿名利用可能にし、設定済みの場合だけGoogle OAuthログインを追加する。
     *
     * @param http Spring Security HTTP設定
     * @param properties Google OAuth設定
     * @param provisioningService ログイン成功時のアカウント作成サービス
     * @return セキュリティフィルターチェーン
     * @throws Exception HTTPセキュリティ構成に失敗した場合
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GoogleOAuthProperties properties,
            GoogleOidcUserProvisioningService provisioningService)
            throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/simulations"));
        if (properties.enabled()) {
            http.oauth2Login(
                    oauth2 ->
                            oauth2.userInfoEndpoint(
                                    userInfo -> userInfo.oidcUserService(provisioningService)));
        }
        http.logout(Customizer.withDefaults());
        return http.build();
    }
}
