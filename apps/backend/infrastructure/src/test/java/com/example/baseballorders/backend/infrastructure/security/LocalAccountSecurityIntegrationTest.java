package com.example.baseballorders.backend.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.baseballorders.backend.application.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 実物: Spring Security、BCrypt、LocalAccountRegistrationController、UserAccountService、Flyway、H2、JPA。
 * モック: HTTPサーブレット環境をMockMvcで代替。 担保する疎通: POST /register -> UserAccountService -> JPA users -> POST
 * /login -> Spring Securityフォーム認証。 担保しないもの: ブラウザのセッションCookie保持とGoogle OIDC。
 */
@SpringBootTest
class LocalAccountSecurityIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy filterChain;
    @Autowired private UserAccountService userAccountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filterChain).build();
    }

    @Test
    @DisplayName("登録したIDとパスワードでフォームログインできる")
    void authenticatesRegisteredLocalAccount() throws Exception {
        // given
        var userId = "local-login-user";
        var password = "secure-password-123";

        // when
        var registration =
                mockMvc.perform(
                        post("/register")
                                .with(csrf())
                                .param("userId", userId)
                                .param("password", password));
        var login =
                mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("userId", userId)
                                .param("password", password));

        // then
        registration.andExpectAll(status().is3xxRedirection(), redirectedUrl("/login?registered"));
        login.andExpectAll(status().is3xxRedirection(), redirectedUrl("/"));
        assertAll(() -> assertTrue(userAccountService.findLocalCredentials(userId).isPresent()));
    }

    @Test
    @DisplayName("形式外のIDでのフォームログインは認証失敗として扱う")
    void rejectsMalformedLocalIdAsAuthenticationFailure() throws Exception {
        // given
        var request =
                post("/login")
                        .with(csrf())
                        .param("userId", "x")
                        .param("password", "secure-password-123");

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpectAll(status().is3xxRedirection(), redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("Flywayの初期ローカルユーザーでログインできる")
    void authenticatesFlywaySeededLocalAccount() throws Exception {
        // given
        var request =
                post("/login").with(csrf()).param("userId", "test").param("password", "password");

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpectAll(status().is3xxRedirection(), redirectedUrl("/"));
    }
}
