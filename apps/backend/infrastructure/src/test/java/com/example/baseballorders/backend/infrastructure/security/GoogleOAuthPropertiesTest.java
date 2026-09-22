package com.example.baseballorders.backend.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoogleOAuthPropertiesTest {

    @Test
    @DisplayName("client IDまたはsecretが空白ならGoogle OAuthログインは無効になる")
    void disablesGoogleOauthWhenCredentialsAreMissing() {
        // given
        var missingClientId = new GoogleOAuthProperties("", "secret");
        var missingSecret = new GoogleOAuthProperties("client-id", " ");

        // when
        var clientIdEnabled = missingClientId.enabled();
        var secretEnabled = missingSecret.enabled();

        // then
        assertAll(() -> assertFalse(clientIdEnabled), () -> assertFalse(secretEnabled));
    }

    @Test
    @DisplayName("client IDとsecretが設定済みならGoogle OAuthログインは有効になる")
    void enablesGoogleOauthWhenCredentialsArePresent() {
        // given
        var properties = new GoogleOAuthProperties("client-id", "client-secret");

        // when
        var enabled = properties.enabled();

        // then
        assertAll(() -> assertTrue(enabled));
    }
}
