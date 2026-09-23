package com.example.baseballorders.backend.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 実物: Spring Security、SimulationPageController、SimulatorRequestController、アプリケーションコンテキスト。 モック:
 * HTTPサーブレット環境をMockMvcで代替。 担保する疎通: HTTP GET / -> Spring Security -> /login、および HTTP POST
 * /simulations -> Spring Security -> /login。 担保しないもの: 認証済みユーザーによるシミュレーション実行と外部SQS通信。
 */
@SpringBootTest
class SimulationAccessSecurityIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy filterChain;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filterChain).build();
    }

    @Test
    @DisplayName("未認証ユーザーは対戦ページへアクセスできない")
    void redirectsUnauthenticatedUserFromSimulationPage() throws Exception {
        // given
        var request = get("/");

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("未認証ユーザーはシミュレーションAPIへアクセスできない")
    void redirectsUnauthenticatedUserFromSimulationApi() throws Exception {
        // given
        var request = post("/simulations").contentType(MediaType.APPLICATION_JSON).content("[]");

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }
}
