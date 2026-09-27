package com.example.baseballorders.backend.infrastructure.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class LogoutSecurityIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy filterChain;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filterChain).build();
    }

    /**
     * 実物: Spring Securityのログアウトフィルターとアプリケーションコンテキスト。モック: HTTPサーブレット環境をMockMvcで代替。 担保する疎通: HTTP POST
     * /logout -> CSRF検証 -> Spring Security LogoutFilter。 担保しないもの:
     * Googleの実際のOIDC認証とブラウザのセッションCookie保持。
     */
    @Test
    @DisplayName("CSRFトークンなしのログアウトPOSTは拒否される")
    void rejectsLogoutWithoutCsrfToken() throws Exception {
        // given
        var request = post("/logout").with(user("authenticated-user"));

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().isForbidden());
    }

    /**
     * 実物: Spring Securityのログアウトフィルターとアプリケーションコンテキスト。モック: HTTPサーブレット環境をMockMvcで代替。 担保する疎通: HTTP POST
     * /logout -> CSRF検証 -> Spring Security LogoutFilter -> リダイレクト。 担保しないもの:
     * Googleの実際のOIDC認証とブラウザのセッションCookie保持。
     */
    @Test
    @DisplayName("CSRFトークン付きのログアウトPOSTはリダイレクトされる")
    void logsOutWithCsrfToken() throws Exception {
        // given
        var request = post("/logout").with(user("authenticated-user")).with(csrf());

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().is3xxRedirection());
    }
}
