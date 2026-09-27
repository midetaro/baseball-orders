package com.example.baseballorders.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.backend.application.UserAccountService;
import com.example.baseballorders.backend.domain.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JpaUserAccountRepositoryIntegrationTest {
    @Autowired private UserAccountService service;

    /**
     * 実物: Spring Boot、Flyway、H2、JPA、JpaUserAccountRepository。 モック: なし。 担保する疎通: UserAccountService
     * -> JpaUserAccountRepository -> Spring Data JPA -> H2 users。 担保しないもの:
     * Googleの実際のOIDC認証と認可コード交換。
     */
    @Test
    @DisplayName("Googleアカウントを保存するとH2が自動採番IDを付与する")
    void savesGoogleAccountWithGeneratedIdentity() {
        // given
        var firstSubject = "persistence-subject-first";
        var secondSubject = "persistence-subject-second";

        // when
        var first = service.provisionGoogleAccount(firstSubject);
        var second = service.provisionGoogleAccount(secondSubject);

        // then
        assertAll(
                () -> assertNotNull(first.id()),
                () -> assertTrue(second.id() > first.id()),
                () -> assertEquals("google:" + firstSubject, first.username()),
                () -> assertEquals(UserStatus.ACTIVE, first.status()));
    }
}
