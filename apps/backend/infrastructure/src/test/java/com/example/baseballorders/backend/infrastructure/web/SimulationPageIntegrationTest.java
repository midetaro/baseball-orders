package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 実物: HTTPサーバー、SimulationPageController、SimulationGuidePageController、Thymeleaf、静的リソース配信。 モック:
 * SqsTemplate。 担保する疎通: ログインなしのHTTP GET / -> SimulationPageController -> Thymeleaf HTML応答。HTTP GET
 * /large-scale および /single-game -> SimulationPageController -> Thymeleaf HTML応答も担保する。分離したHTTP GET
 * /css/simulation.css、/css/single-game.css、/js/simulation.js および /js/single-game.js -> 静的リソース配信 ->
 * CSS・JS応答も担保する。担保しないもの: SQSへのシミュレーション要求送信と結果受信、入力値のブラウザ操作。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.aws.sqs.enabled=false")
class SimulationPageIntegrationTest {

    // GUI表示では使用しないSqsTemplateをモックする
    @MockitoBean private SqsTemplate sqsTemplate;

    @LocalServerPort private int port;

    private static void assertContainsPattern(String actual, String pattern) {
        assertTrue(Pattern.compile(pattern).matcher(actual).find());
    }

    @Test
    @DisplayName("ログインなしでトップ画面へアクセスするとリダイレクトされずに表示される")
    void rendersSimulationPageWithoutLogin() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();

        // when
        HttpResponse<String> response;
        try (var client =
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertFalse(response.body().contains("ログイン")));
    }

    @Test
    @DisplayName("1試合実行画面へアクセスすると打順設定画面がHTMLで表示される")
    void rendersSingleGamePage() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/single-game"))
                        .GET()
                        .build();

        // when
        HttpResponse<String> response;
        HttpResponse<String> cssResponse;
        HttpResponse<String> jsResponse;
        try (var client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            cssResponse =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/css/single-game.css"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            jsResponse =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/js/single-game.js"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("打順入力")),
                () -> assertTrue(response.body().contains("<title>打順監督</title>")),
                () -> assertTrue(response.body().contains("<h1>打順監督</h1>")),
                () -> assertTrue(response.body().contains("1試合を実行")),
                () -> assertTrue(response.body().contains("id=\"frame-stage\"")),
                () -> assertTrue(response.body().contains("data-frame-duration-millis=\"1000\"")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains("data-hit-frame-duration-millis=\"1800\"")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains("data-score-frame-duration-millis=\"2200\"")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains("data-home-run-frame-duration-millis=\"3600\"")),
                () -> assertTrue(jsResponse.body().contains("function classifyEffect(")),
                () -> assertTrue(cssResponse.body().contains("@keyframes home-run-headline")),
                () -> assertTrue(response.body().contains("id=\"order-table-scroll\"")),
                () -> assertTrue(response.body().contains("href=\"/large-scale\"")),
                () -> assertTrue(response.body().contains("class=\"view-tabs\" role=\"tablist\"")),
                () -> assertTrue(response.body().contains("id=\"input-view\" role=\"tabpanel\"")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "<section aria-labelledby=\"results-heading\""
                                                        + " hidden id=\"results\" role=\"tabpanel\">")),
                () -> assertTrue(response.body().contains("id=\"edit-lineup\"")),
                () -> assertFalse(response.body().contains("id=\"toggle-lineup\"")),
                () -> assertFalse(cssResponse.body().contains("BASEBALL ORDER LAB")),
                () -> assertTrue(jsResponse.body().contains("function showView(resultsVisible)")),
                () -> assertFalse(response.body().contains("<style>")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "<link rel=\"stylesheet\""
                                                        + " href=\"/css/single-game.css\">")),
                () -> assertEquals(200, cssResponse.statusCode()),
                () -> assertTrue(cssResponse.body().contains(".out-count {")),
                () -> assertTrue(cssResponse.body().contains("color-scheme: light")),
                () -> assertTrue(cssResponse.body().contains("--paper: #f3eee2")),
                () -> assertTrue(cssResponse.body().contains("--moss: #56704a")),
                () -> assertFalse(cssResponse.body().contains("--cyan")),
                () -> assertFalse(cssResponse.body().contains("--lime")),
                () -> assertFalse(response.body().contains("<script th:inline=\"none\">")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "<script"
                                                        + " src=\"/js/single-game.js\"></script>")),
                () -> assertEquals(200, jsResponse.statusCode()),
                () -> assertTrue(jsResponse.body().contains("fetch('/simulations/single-game'")),
                () -> assertTrue(jsResponse.body().contains("data.transitions")),
                () -> assertTrue(jsResponse.body().contains("renderGame(data.transitions)")),
                () -> assertFalse(jsResponse.body().contains("data.statistics")));
    }

    @Test
    @DisplayName("大規模実行画面へアクセスすると打者一覧と打順設定画面がHTMLで表示される")
    void rendersSimulationPage() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();

        // when
        HttpResponse<String> response;
        HttpResponse<String> cssResponse;
        HttpResponse<String> jsResponse;
        try (var client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            cssResponse =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/css/simulation.css"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            jsResponse =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/js/simulation.js"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("打順入力")),
                () -> assertTrue(response.body().contains("<title>打順監督</title>")),
                () -> assertTrue(response.body().contains("<h1>打順監督</h1>")),
                () -> assertFalse(response.body().contains("ログイン")),
                () -> assertFalse(response.body().contains("<style>")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "<link rel=\"stylesheet\""
                                                        + " href=\"/css/simulation.css\">")),
                () -> assertEquals(200, cssResponse.statusCode()),
                () -> assertFalse(response.body().contains("<script>")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "<script"
                                                        + " src=\"/js/simulation.js\"></script>")),
                () -> assertEquals(200, jsResponse.statusCode()),
                () -> assertTrue(response.body().contains("出塁率")),
                () -> assertTrue(response.body().contains("長打率")),
                () ->
                        assertContainsPattern(
                                jsResponse.body(), "position\\.textContent=`\\$\\{index\\+\\d+}番`"),
                () ->
                        assertContainsPattern(
                                jsResponse.body(),
                                "hitAverage:'\\d+\\.\\d{2}',sluggish:'\\d+\\.\\d{2}'"),
                () ->
                        assertTrue(
                                jsResponse
                                        .body()
                                        .matches(
                                                "(?s).*key:'hitAverage',label:'出塁率',min:\\d+\\.\\d+,max:\\d+\\.\\d+.*")),
                () ->
                        assertTrue(
                                jsResponse
                                        .body()
                                        .matches(
                                                "(?s).*key:'sluggish',label:'長打率',min:\\d+\\.\\d+,max:\\d+\\.\\d+.*")),
                () -> assertFalse(jsResponse.body().contains("盗塁成功率")),
                () -> assertFalse(jsResponse.body().contains("バント成功率")),
                () -> assertFalse(jsResponse.body().contains("stealSuccessRate")),
                () -> assertFalse(jsResponse.body().contains("buntSuccessRate")),
                () -> assertTrue(response.body().contains("SIMULATIONを実行")),
                () -> assertTrue(response.body().contains("<h2 id=\"order-heading\">打順入力</h2>")),
                () -> assertTrue(response.body().contains("id=\"toggle-all-bunt\"")),
                () -> assertTrue(response.body().contains("id=\"toggle-all-steal\"")),
                () ->
                        assertTrue(
                                jsResponse
                                        .body()
                                        .contains("lineup.every(player=>player.buntEnabled)")),
                () ->
                        assertTrue(
                                jsResponse
                                        .body()
                                        .contains("lineup.every(player=>player.stealEnabled)")),
                () -> assertTrue(response.body().contains("href=\"/simulation-guide\"")),
                () -> assertFalse(response.body().toLowerCase().contains("pitcher")),
                () -> assertFalse(jsResponse.body().toLowerCase().contains("pitcher")),
                () -> assertTrue(jsResponse.body().contains("function validLineup()")),
                () -> assertFalse(jsResponse.body().contains("lineup.length<=0.35")),
                () -> assertFalse(jsResponse.body().contains("lineup.length<=0.4")),
                () -> assertTrue(response.body().contains("本塁打")),
                () -> assertTrue(jsResponse.body().contains("ソロ")),
                () -> assertTrue(jsResponse.body().contains("ツーラン")),
                () -> assertTrue(jsResponse.body().contains("スリーラン")),
                () -> assertTrue(jsResponse.body().contains("満塁")),
                () -> assertTrue(response.body().contains("バント")),
                () -> assertTrue(response.body().contains("盗塁")),
                () -> assertTrue(response.body().contains("得点サマリー")),
                () -> assertTrue(response.body().contains("本塁打の内訳")),
                () -> assertTrue(!response.body().contains("戦術の成否")),
                () -> assertTrue(response.body().contains("id=\"share-results\"")),
                () -> assertTrue(jsResponse.body().contains("navigator.share")),
                () -> assertTrue(jsResponse.body().contains("clipboard.writeText")),
                () ->
                        assertTrue(
                                cssResponse
                                        .body()
                                        .matches("(?s).*\\.order \\{.*width:\\s*max-content;.*")),
                () ->
                        assertContainsPattern(
                                cssResponse.body(),
                                "grid-template-columns:\\d+px\\s+repeat\\(\\d+,\\s*\\d+px\\)\\s+\\d+px\\s+\\d+px"),
                () -> assertTrue(jsResponse.body().contains("function fieldWrapper(")),
                () ->
                        assertTrue(
                                jsResponse
                                        .body()
                                        .contains(
                                                "input.disabled=inFlight || (field.enabledKey && !player[field.enabledKey]);")),
                () -> assertContainsPattern(jsResponse.body(), "input\\.step='\\d+\\.\\d+'"),
                () ->
                        assertTrue(
                                jsResponse
                                        .body()
                                        .contains(
                                                "input.value.startsWith('.') ? `0${input.value}` : input.value")),
                () -> assertTrue(cssResponse.body().contains(".section-head {")),
                () -> assertFalse(jsResponse.body().contains("hasAtMostTwoDecimalPlaces")),
                () -> assertTrue(response.body().contains("class=\"simulation-workspace\"")),
                () -> assertTrue(response.body().contains("id=\"input-view\" role=\"tabpanel\"")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "<section aria-labelledby=\"results-heading\""
                                                        + " hidden id=\"results\" role=\"tabpanel\">")),
                () -> assertTrue(response.body().contains("id=\"edit-lineup\"")),
                () -> assertFalse(response.body().contains("id=\"toggle-lineup\"")),
                () -> assertTrue(jsResponse.body().contains("function showView(resultsVisible)")),
                () -> assertTrue(cssResponse.body().contains("--moss: #56704a")),
                () -> assertTrue(jsResponse.body().contains("'homeRunCount'")),
                () -> assertTrue(jsResponse.body().contains("scoreDistribution")),
                () -> assertTrue(response.body().contains("score-histogram")),
                () -> assertTrue(response.body().contains("score-distribution-axis")),
                () -> assertTrue(response.body().contains("全試合に対する割合")),
                () ->
                        assertContainsPattern(
                                jsResponse.body(), "Math\\.ceil\\(maximumRate / \\d+\\) \\* \\d+"),
                () -> assertContainsPattern(jsResponse.body(), "rate / histogramMaximum \\* \\d+"),
                () -> assertContainsPattern(jsResponse.body(), "histogramMaximum - index \\* \\d+"),
                () -> assertTrue(response.body().contains("home-run-breakdown")),
                () -> assertTrue(response.body().contains("home-run-legend")),
                () -> assertTrue(response.body().contains("本塁打なし")),
                () -> assertTrue(response.body().contains("バントの内訳")),
                () -> assertTrue(jsResponse.body().contains("進塁成功")),
                () -> assertTrue(jsResponse.body().contains("スクイズ成功")),
                () -> assertTrue(jsResponse.body().contains("進塁失敗")),
                () -> assertTrue(jsResponse.body().contains("スクイズ失敗")),
                () -> assertTrue(response.body().contains("盗塁の内訳")),
                () -> assertTrue(jsResponse.body().contains("二盗成功")),
                () -> assertTrue(jsResponse.body().contains("三盗成功")),
                () -> assertTrue(response.body().contains("id=\"bunt-count\"")),
                () -> assertTrue(response.body().contains("id=\"bunt-failure-count\"")),
                () -> assertTrue(response.body().contains("id=\"steal-count\"")),
                () -> assertTrue(response.body().contains("id=\"steal-failure-count\"")),
                () -> assertTrue(jsResponse.body().contains("const detailTotal=details.reduce")),
                () -> assertTrue(jsResponse.body().contains("Number(count)/detailTotal*100")),
                () -> assertTrue(!response.body().contains("tactics-comparison")));
    }

    @Test
    @DisplayName("大規模実行画面は選手性格を選択して全員をデフォルトへ初期化できる")
    void rendersPlayerPersonalityControls() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/large-scale"))
                        .GET()
                        .build();

        // when
        HttpResponse<String> response;
        HttpResponse<String> jsResponse;
        try (var client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            jsResponse =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/js/simulation.js"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("id=\"reset-all-personalities\"")),
                () -> assertTrue(response.body().contains("性格")),
                () -> assertEquals(200, jsResponse.statusCode()),
                () -> assertTrue(jsResponse.body().contains("DEFAULT:'標準'")),
                () -> assertTrue(jsResponse.body().contains("EAGER_SLUGGISH:'長距離砲'")),
                () -> assertFalse(jsResponse.body().contains("EAGER_SLUGGISH:'長打重視'")),
                () -> assertTrue(jsResponse.body().contains("EAGER_STEAL:'盗塁重視'")),
                () -> assertTrue(jsResponse.body().contains("EAGER_BUNT:'バント重視'")),
                () -> assertTrue(jsResponse.body().contains("personality:player.personality")));
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
                () -> assertFalse(response.body().contains("ログイン")),
                () -> assertContainsPattern(response.body(), "盗塁判定\\s*→\\s*バント判定\\s*→\\s*通常打撃"),
                () -> assertTrue(response.body().contains("各選手の入力項目")),
                () -> assertTrue(response.body().contains("出塁率")),
                () -> assertTrue(response.body().contains("長打率")),
                () -> assertFalse(response.body().contains("バント成功率")),
                () -> assertFalse(response.body().contains("<strong>0.00〜0.95</strong>")),
                () -> assertFalse(response.body().contains("盗塁成功率")),
                () -> assertTrue(response.body().contains("バント・盗塁のオン／オフ")),
                () -> assertTrue(response.body().contains("性格による違い")),
                () -> assertTrue(response.body().contains("標準")),
                () -> assertTrue(response.body().contains("盗塁は通常の頻度、バントは無死のときに試みます")),
                () -> assertTrue(response.body().contains("長距離砲")),
                () -> assertTrue(response.body().contains("標準より本塁打の割合が増える")),
                () -> assertTrue(response.body().contains("盗塁重視")),
                () -> assertTrue(response.body().contains("標準より二塁走者の盗塁を試みやすくなります")),
                () -> assertTrue(response.body().contains("バント重視")),
                () -> assertTrue(response.body().contains("一死でもバントを試みます")),
                () -> assertTrue(response.body().contains("平均得点")),
                () -> assertTrue(response.body().contains("盗塁の成功率は<strong>70%</strong>")),
                () -> assertTrue(response.body().contains("盗塁死となり、アウトが一つ増えます")),
                () -> assertTrue(response.body().contains("進塁バント")),
                () -> assertTrue(response.body().contains("成功率は<strong>81%</strong>")),
                () -> assertTrue(response.body().contains("<strong>25%</strong>の確率で試み")),
                () -> assertTrue(response.body().contains("試みた場合の成功率は<strong>45%</strong>")),
                () -> assertTrue(response.body().contains("挑戦しなかった場合は通常の打撃を行います")),
                () -> assertTrue(response.body().contains("満塁で四球になると押し出しで1点入ります")),
                () -> assertTrue(response.body().contains("進塁バントとスクイズの成功・失敗をそれぞれ色分け")),
                () -> assertTrue(response.body().contains("二盗成功・三盗成功・盗塁失敗を色分け")),
                () -> assertTrue(response.body().contains("打順を組み立てる")),
                () -> assertTrue(response.body().contains("color-scheme: light")),
                () -> assertTrue(response.body().contains("--paper: #f3eee2")),
                () -> assertTrue(response.body().contains("--moss: #56704a")),
                () -> assertTrue(response.body().contains("--clay: #bf6b45")),
                () -> assertFalse(response.body().contains("--cyan")),
                () -> assertFalse(response.body().contains("--pink")));
    }
}
