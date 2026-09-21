package com.example.baseballorders.backend.infrastructure.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/** セッション認証、CSRF、Cookie保護を設定する。 */
@Configuration
public class SecurityConfiguration {
    /**
     * HTTPエンドポイントの公開範囲と認証規則を構成する。
     *
     * @param http Spring Security HTTP構成
     * @return 構成済みフィルタチェーン
     * @throws Exception Spring Security構成に失敗した場合
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(
                        csrf ->
                                csrf.csrfTokenRepository(
                                                CookieCsrfTokenRepository.withHttpOnlyFalse())
                                        .ignoringRequestMatchers("/simulations"))
                .authorizeHttpRequests(
                        requests ->
                                requests.requestMatchers(
                                                "/",
                                                "/simulation-guide",
                                                "/simulations",
                                                "/login",
                                                "/users/new",
                                                "/users")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(new SessionAuthenticationFilter(), AuthorizationFilter.class)
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"));
        return http.build();
    }
}
