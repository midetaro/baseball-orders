package com.example.baseballorders.backend.infrastructure.security;

import com.example.baseballorders.backend.application.UserAccountService;
import com.example.baseballorders.backend.domain.UserStatus;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
     * ローカルアカウントのパスワードをBCryptでハッシュ化する。
     *
     * @return BCryptパスワードエンコーダー
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * フォームログイン用にローカルアカウントを読み込む。
     *
     * @param userAccountService ローカル認証情報を取得するユースケース
     * @return Spring Securityのユーザー詳細サービス
     */
    @Bean
    public UserDetailsService userDetailsService(UserAccountService userAccountService) {
        return userId -> {
            try {
                return userAccountService
                        .findLocalCredentials(userId)
                        .map(
                                credentials ->
                                        User.withUsername(userId)
                                                .password(credentials.passwordHash())
                                                .disabled(credentials.status() != UserStatus.ACTIVE)
                                                .roles("USER")
                                                .build())
                        .orElseThrow(() -> new UsernameNotFoundException("local user not found"));
            } catch (IllegalArgumentException _) {
                throw new UsernameNotFoundException("local user not found");
            }
        };
    }

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
     * 公開ページ以外を認証必須にし、ローカルフォームログインと設定済みのGoogle OAuthログインを追加する。
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
        http.authorizeHttpRequests(
                authorize ->
                        authorize
                                .requestMatchers(
                                        "/login",
                                        "/register",
                                        "/simulation-guide",
                                        "/css/**",
                                        "/js/**")
                                .permitAll()
                                .anyRequest()
                                .authenticated());
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/simulations"));
        http.formLogin(form -> form.loginPage("/login").usernameParameter("userId").permitAll());
        if (properties.enabled()) {
            http.oauth2Login(
                    oauth2 ->
                            oauth2.loginPage("/login")
                                    .userInfoEndpoint(
                                            userInfo ->
                                                    userInfo.oidcUserService(provisioningService)));
        }
        http.logout(Customizer.withDefaults());
        return http.build();
    }
}
