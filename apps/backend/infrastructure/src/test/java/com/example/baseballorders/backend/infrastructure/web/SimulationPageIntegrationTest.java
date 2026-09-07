package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 実物: HTTPサーバー、Controller、Thymeleaf、H2の選手データ。 モック: SqsTemplate。 担保する疎通: HTTP GET ->
 * SimulationPageController -> Thymeleaf HTML応答。 担保しないもの: SQSへのシミュレーション要求送信と結果受信。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.cloud.aws.sqs.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:simulation-page"
        })
class SimulationPageIntegrationTest {

    // GUI表示では使用しないSqsTemplateをモックする
    @MockitoBean private SqsTemplate sqsTemplate;

    @LocalServerPort private int port;

    @Test
    @DisplayName("トップ画面へアクセスすると打者一覧と打順設定画面がHTMLで表示される")
    void rendersSimulationPage() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();

        // when
        HttpResponse<String> response;
        try (var client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("打者一覧")),
                () -> assertTrue(response.body().contains("ルンバ")),
                () -> assertTrue(response.body().contains("打率")),
                () -> assertTrue(response.body().contains("長打率")),
                () -> assertTrue(response.body().contains("盗塁成功率")),
                () -> assertTrue(response.body().contains("SIMULATIONを実行")),
                () -> assertTrue(response.body().contains("href=\"/simulation-guide\"")),
                () -> assertTrue(response.body().contains("fetch('/simulations'")));
    }

    @Test
    @DisplayName("シミュレーションの仕組みページへアクセスするとロジックの説明がHTMLで表示される")
    void rendersSimulationGuidePage() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/simulation-guide"))
                        .GET()
                        .build();

        // when
        HttpResponse<String> response;
        try (var client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("シミュレーションの仕組み")),
                () -> assertTrue(response.body().contains("9回")),
                () -> assertTrue(response.body().contains("平均得点")),
                () -> assertTrue(response.body().contains("打順を組み立てる")));
    }
}
