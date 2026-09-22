package com.example.baseballorders.backend.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 環境変数から受け取るGoogle OAuthクライアント設定。 */
@ConfigurationProperties("baseball-orders.security.google")
public record GoogleOAuthProperties(String clientId, String clientSecret) {

    /**
     * Google OAuthログインを有効にできる設定かを返す。
     *
     * @return client IDとclient secretの双方が設定されている場合true
     */
    public boolean enabled() {
        return clientId != null
                && !clientId.isBlank()
                && clientSecret != null
                && !clientSecret.isBlank();
    }
}
