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
 * 実物: HTTPサーバー、SimulationPageController、SimulationGuidePageController、Thymeleaf。 モック: SqsTemplate。
 * 担保する疎通: HTTP GET -> 各PageController -> Thymeleaf HTML応答。 担保しないもの: SQSへのシミュレーション要求送信と結果受信、
 * 入力値のブラウザ操作。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.aws.sqs.enabled=false")
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
                () -> assertTrue(response.body().contains("打順入力")),
                () -> assertTrue(response.body().contains("<title>打順監督</title>")),
                () -> assertTrue(response.body().contains("<h1>打順監督</h1>")),
                () -> assertTrue(!response.body().contains("Baseball Orders / Simulator")),
                () -> assertTrue(!response.body().contains("LINEUP<br>BUILDER")),
                () -> assertTrue(response.body().contains("出塁率")),
                () -> assertTrue(response.body().contains("長打率")),
                () -> assertTrue(response.body().contains("盗塁成功率")),
                () -> assertTrue(response.body().contains("position.textContent=`${index+1}番`")),
                () -> assertTrue(!response.body().contains("name:")),
                () -> assertTrue(response.body().contains("hitAverage:'0.32',sluggish:'0.35'")),
                () -> assertTrue(response.body().contains("hitAverage:'0.37',sluggish:'0.50'")),
                () -> assertTrue(response.body().contains("hitAverage:'0.28',sluggish:'0.40'")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains("key:'hitAverage',label:'出塁率',min:0.01,max:0.6")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains("key:'sluggish',label:'長打率',min:0.1,max:0.6")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "key:'stealSuccessRate',label:'盗塁成功率',min:0.1,max:0.9")),
                () -> assertTrue(response.body().contains("SIMULATIONを実行")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "<h2 id=\"order-heading\">打順入力</h2><button class=\"submit\"")),
                () -> assertTrue(response.body().contains("id=\"toggle-all-bunt\"")),
                () -> assertTrue(response.body().contains("id=\"toggle-all-steal\"")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains("lineup.every(player=>player.buntEnabled)")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains("lineup.every(player=>player.stealEnabled)")),
                () -> assertTrue(response.body().contains("href=\"/simulation-guide\"")),
                () -> assertTrue(response.body().contains("fetch('/simulations'")),
                () -> assertTrue(response.body().contains("本塁打")),
                () -> assertTrue(response.body().contains("ソロ")),
                () -> assertTrue(response.body().contains("ツーラン")),
                () -> assertTrue(response.body().contains("スリーラン")),
                () -> assertTrue(response.body().contains("満塁")),
                () -> assertTrue(response.body().contains("バント")),
                () -> assertTrue(response.body().contains("盗塁")),
                () -> assertTrue(response.body().contains("得点サマリー")),
                () -> assertTrue(response.body().contains("本塁打の内訳")),
                () -> assertTrue(response.body().contains("戦術の成否")),
                () -> assertTrue(response.body().contains("id=\"share-results\"")),
                () -> assertTrue(response.body().contains("navigator.share")),
                () -> assertTrue(response.body().contains("clipboard.writeText")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(".order { width:max-content; min-width:570px;")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "grid-template-columns:38px repeat(4,76px) 118px 78px 78px")),
                () -> assertTrue(response.body().contains("enabledKey:'buntEnabled'")),
                () -> assertTrue(response.body().contains("enabledKey:'stealEnabled'")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "input.disabled=inFlight || (field.enabledKey && !player[field.enabledKey]);")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "buntSuccessRate:'0.80',stealSuccessRate:'0.80'")),
                () -> assertTrue(response.body().contains("input.step='0.01'")),
                () -> assertTrue(!response.body().contains("hitAverage:'.32'")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "input.value.startsWith('.') ? `0${input.value}` : input.value")),
                () -> assertTrue(response.body().contains(".section-head { display:flex;")),
                () -> assertTrue(response.body().contains("hasAtMostTwoDecimalPlaces")),
                () -> assertTrue(response.body().contains("class=\"simulation-workspace\"")),
                () ->
                        assertTrue(
                                !response.body()
                                        .contains(
                                                "id=\"results\" aria-labelledby=\"results-heading\" hidden")),
                () -> assertTrue(!response.body().contains("id=\"home-run-empty-state\" hidden")),
                () -> assertTrue(response.body().contains("'homeRunCount'")),
                () -> assertTrue(response.body().contains("scoreDistribution")),
                () -> assertTrue(response.body().contains("score-histogram")),
                () -> assertTrue(response.body().contains("score-distribution-axis")),
                () -> assertTrue(response.body().contains("全試合に対する割合")),
                () -> assertTrue(response.body().contains("Math.ceil(maximumRate / 25) * 25")),
                () -> assertTrue(response.body().contains("rate / histogramMaximum * 100")),
                () -> assertTrue(response.body().contains("histogramMaximum - index * 25")),
                () -> assertTrue(response.body().contains("home-run-breakdown")),
                () -> assertTrue(response.body().contains("home-run-legend")),
                () -> assertTrue(response.body().contains("本塁打なし")),
                () -> assertTrue(response.body().contains("tactics-comparison")));
    }

    @Test
    @DisplayName("トップ画面は選手性格を選択して全員をデフォルトへ初期化できる")
    void rendersPlayerPersonalityControls() throws Exception {
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
                () -> assertTrue(response.body().contains("id=\"reset-all-personalities\"")),
                () -> assertTrue(response.body().contains("性格")),
                () -> assertTrue(response.body().contains("EagerSluggish")),
                () -> assertTrue(response.body().contains("EagerSteal")),
                () -> assertTrue(response.body().contains("EagerBunt")),
                () -> assertTrue(response.body().contains("personality:player.personality")));
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
                () -> assertTrue(response.body().contains("盗塁死となり、アウトが一つ増えます")),
                () -> assertTrue(response.body().contains("三塁走者がいない一・二塁の状況で、無死の場合だけ")),
                () -> assertTrue(response.body().contains("打順を組み立てる")));
    }
}
