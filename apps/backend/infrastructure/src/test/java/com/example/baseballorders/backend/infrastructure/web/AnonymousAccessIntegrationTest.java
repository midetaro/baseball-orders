package com.example.baseballorders.backend.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 実物:
 * SimulationPageController、SimulationGuidePageController、SimulatorRequestController、アプリケーションコンテキスト。
 * モック: HTTPサーブレット環境をMockMvcで代替。 担保する疎通: ログインなしのHTTP GET
 * /・/single-game・/large-scale・/simulation-guide -> Controller -> Thymeleaf
 * HTML応答、ログインなし・CSRFトークンなしのHTTP POST /simulations/single-game -> SimulatorRequestController ->
 * 入力検証エラー応答、およびHTTP GET /login -> 404。 担保しないもの: SQSへのシミュレーション要求送信と結果受信。
 */
@SpringBootTest
class AnonymousAccessIntegrationTest {

    @Autowired private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("ログインなしで各画面を表示できる")
    void rendersPagesWithoutLogin() throws Exception {
        for (var path : new String[] {"/", "/single-game", "/large-scale", "/simulation-guide"}) {
            // given
            var request = get(path);

            // when
            var result = mockMvc.perform(request);

            // then
            result.andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("ログインなし・CSRFトークンなしでもシミュレーションAPIは入力検証まで到達する")
    void reachesSimulationApiWithoutLoginOrCsrfToken() throws Exception {
        // given
        var request =
                post("/simulations/single-game")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{");

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ログイン画面は提供しない")
    void doesNotServeLoginPage() throws Exception {
        // given
        var request = get("/login");

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().isNotFound());
    }
}
