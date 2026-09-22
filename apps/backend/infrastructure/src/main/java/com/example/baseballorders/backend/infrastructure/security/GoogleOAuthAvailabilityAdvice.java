package com.example.baseballorders.backend.infrastructure.security;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Thymeleaf画面へGoogle OAuthログインの利用可否を公開する。 */
@ControllerAdvice
public class GoogleOAuthAvailabilityAdvice {
    private final GoogleOAuthProperties properties;

    /**
     * 利用可否を公開するAdviceを作成する。
     *
     * @param properties Google OAuth設定
     */
    public GoogleOAuthAvailabilityAdvice(GoogleOAuthProperties properties) {
        this.properties = properties;
    }

    /**
     * Googleログインリンクを表示可能かを返す。
     *
     * @return Google OAuthが設定済みの場合true
     */
    @ModelAttribute("googleOauthEnabled")
    public boolean googleOauthEnabled() {
        return properties.enabled();
    }
}
