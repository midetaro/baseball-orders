package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 実物: HTTPサーバー、Spring Security、ローカルフォームログイン、SimulationPageController、SimulationGuidePageController、
 * LoginPageController、Thymeleaf。 モック: SqsTemplate。 担保する疎通: HTTP GET /login -> HTTP POST /login ->
 * 認証済みHTTP GET / -> Spring Security -> SimulationPageController -> Thymeleaf HTML応答。認証済みHTTP GET
 * /large-scale -> Spring Security -> SimulationPageController -> Thymeleaf HTML応答も担保する。担保しないもの:
 * SQSへのシミュレーション要求送信と結果受信、入力値のブラウザ操作。
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

    private HttpClient authenticatedClient() throws Exception {
        var client =
                HttpClient.newBuilder()
                        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        var loginPage =
                client.send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        var csrfMatcher =
                Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(loginPage.body());
        assertTrue(csrfMatcher.find());
        var form =
                "userId=test&password=password&_csrf="
                        + URLEncoder.encode(csrfMatcher.group(1), StandardCharsets.UTF_8);
        var login =
                client.send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString(form))
                                .build(),
                        HttpResponse.BodyHandlers.discarding());
        assertEquals(302, login.statusCode());
        return client;
    }

    @Test
    @DisplayName("未認証のトップ画面アクセスはログイン画面へリダイレクトされる")
    void redirectsUnauthenticatedSimulationPageAccess() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();

        // when
        HttpResponse<Void> response;
        try (var client =
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            response = client.send(request, HttpResponse.BodyHandlers.discarding());
        }

        // then
        assertAll(
                () -> assertEquals(302, response.statusCode()),
                () ->
                        assertTrue(
                                response.headers()
                                        .firstValue("location")
                                        .orElseThrow()
                                        .contains("/login")));
    }

    @Test
    @DisplayName("トップ画面へアクセスすると1試合実行用の打順設定画面がHTMLで表示される")
    void rendersSingleGamePage() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();

        // when
        HttpResponse<String> response;
        try (var client = authenticatedClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("打順入力")),
                () -> assertTrue(response.body().contains("<title>打順監督</title>")),
                () -> assertTrue(response.body().contains("<h1>打順監督</h1>")),
                () -> assertTrue(response.body().contains("1試合を実行")),
                () -> assertTrue(response.body().contains("id=\"transitions\"")),
                () -> assertTrue(response.body().contains("fetch('/simulations/single-game'")),
                () -> assertTrue(response.body().contains("data.transitions")),
                () -> assertTrue(response.body().contains("href=\"/large-scale\"")),
                () -> assertFalse(response.body().contains("data.statistics")));
    }

    @Test
    @DisplayName("大規模実行画面へアクセスすると打者一覧と打順設定画面がHTMLで表示される")
    void rendersSimulationPage() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/large-scale"))
                        .GET()
                        .build();

        // when
        HttpResponse<String> response;
        try (var client = authenticatedClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("打順入力")),
                () -> assertTrue(response.body().contains("<title>打順監督</title>")),
                () -> assertTrue(response.body().contains("<h1>打順監督</h1>")),
                () -> assertTrue(response.body().contains("ログイン中（")),
                () -> assertTrue(response.body().contains("出塁率")),
                () -> assertTrue(response.body().contains("長打率")),
                () -> assertTrue(response.body().contains("盗塁成功率")),
                () ->
                        assertContainsPattern(
                                response.body(), "position\\.textContent=`\\$\\{index\\+\\d+}番`"),
                () ->
                        assertContainsPattern(
                                response.body(),
                                "hitAverage:'\\d+\\.\\d{2}',sluggish:'\\d+\\.\\d{2}'"),
                () ->
                        assertTrue(
                                response.body()
                                        .matches(
                                                "(?s).*key:'hitAverage',label:'出塁率',min:\\d+\\.\\d+,max:\\d+\\.\\d+.*")),
                () ->
                        assertTrue(
                                response.body()
                                        .matches(
                                                "(?s).*key:'sluggish',label:'長打率',min:\\d+\\.\\d+,max:\\d+\\.\\d+.*")),
                () ->
                        assertContainsPattern(
                                response.body(),
                                "key:'stealSuccessRate',label:'盗塁成功率',min:\\d+\\.\\d+,max:\\d+\\.\\d+"),
                () -> assertTrue(response.body().contains("buntSuccessRate:[0,0.7]")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "key:'buntSuccessRate',label:'バント成功率',min:0,max:0.7")),
                () -> assertTrue(response.body().contains("SIMULATIONを実行")),
                () -> assertTrue(response.body().contains("<h2 id=\"order-heading\">打順入力</h2>")),
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
                () -> assertFalse(response.body().toLowerCase().contains("pitcher")),
                () -> assertTrue(response.body().contains("function validLineup()")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "lineup.reduce((sum,player)=>sum+Number(player.hitAverage),0)/lineup.length<=0.35")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "lineup.reduce((sum,player)=>sum+Number(player.sluggish),0)/lineup.length<=0.4")),
                () -> assertTrue(response.body().contains("本塁打")),
                () -> assertTrue(response.body().contains("ソロ")),
                () -> assertTrue(response.body().contains("ツーラン")),
                () -> assertTrue(response.body().contains("スリーラン")),
                () -> assertTrue(response.body().contains("満塁")),
                () -> assertTrue(response.body().contains("バント")),
                () -> assertTrue(response.body().contains("盗塁")),
                () -> assertTrue(response.body().contains("得点サマリー")),
                () -> assertTrue(response.body().contains("本塁打の内訳")),
                () -> assertTrue(!response.body().contains("戦術の成否")),
                () -> assertTrue(response.body().contains("id=\"share-results\"")),
                () -> assertTrue(response.body().contains("navigator.share")),
                () -> assertTrue(response.body().contains("clipboard.writeText")),
                () ->
                        assertTrue(
                                response.body()
                                        .matches(
                                                "(?s).*\\.order \\{.*width:\\s*max-content;.*min-width:\\s*\\d+px;.*")),
                () ->
                        assertContainsPattern(
                                response.body(),
                                "grid-template-columns:\\d+px\\s+repeat\\(\\d+,\\s*\\d+px\\)\\s+\\d+px\\s+\\d+px"),
                () -> assertTrue(response.body().contains("enabledKey:'buntEnabled'")),
                () -> assertTrue(response.body().contains("enabledKey:'stealEnabled'")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "input.disabled=inFlight || (field.enabledKey && !player[field.enabledKey]);")),
                () ->
                        assertContainsPattern(
                                response.body(),
                                "buntSuccessRate:'\\d+\\.\\d{2}',stealSuccessRate:'\\d+\\.\\d{2}'"),
                () -> assertContainsPattern(response.body(), "input\\.step='\\d+\\.\\d+'"),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "input.value.startsWith('.') ? `0${input.value}` : input.value")),
                () -> assertTrue(response.body().contains(".section-head {")),
                () -> assertTrue(response.body().contains("hasAtMostTwoDecimalPlaces")),
                () -> assertTrue(response.body().contains("class=\"simulation-workspace\"")),
                () -> assertTrue(response.body().contains("'homeRunCount'")),
                () -> assertTrue(response.body().contains("scoreDistribution")),
                () -> assertTrue(response.body().contains("score-histogram")),
                () -> assertTrue(response.body().contains("score-distribution-axis")),
                () -> assertTrue(response.body().contains("全試合に対する割合")),
                () ->
                        assertContainsPattern(
                                response.body(), "Math\\.ceil\\(maximumRate / \\d+\\) \\* \\d+"),
                () -> assertContainsPattern(response.body(), "rate / histogramMaximum \\* \\d+"),
                () -> assertContainsPattern(response.body(), "histogramMaximum - index \\* \\d+"),
                () -> assertTrue(response.body().contains("home-run-breakdown")),
                () -> assertTrue(response.body().contains("home-run-legend")),
                () -> assertTrue(response.body().contains("本塁打なし")),
                () -> assertTrue(response.body().contains("バントの内訳")),
                () -> assertTrue(response.body().contains("進塁成功")),
                () -> assertTrue(response.body().contains("スクイズ成功")),
                () -> assertTrue(response.body().contains("進塁失敗")),
                () -> assertTrue(response.body().contains("スクイズ失敗")),
                () -> assertTrue(response.body().contains("盗塁の内訳")),
                () -> assertTrue(response.body().contains("二盗成功")),
                () -> assertTrue(response.body().contains("三盗成功")),
                () -> assertTrue(response.body().contains("id=\"bunt-count\"")),
                () -> assertTrue(response.body().contains("id=\"bunt-failure-count\"")),
                () -> assertTrue(response.body().contains("id=\"steal-count\"")),
                () -> assertTrue(response.body().contains("id=\"steal-failure-count\"")),
                () -> assertTrue(response.body().contains("const detailTotal=details.reduce")),
                () -> assertTrue(response.body().contains("Number(count)/detailTotal*100")),
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
        try (var client = authenticatedClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("id=\"reset-all-personalities\"")),
                () -> assertTrue(response.body().contains("性格")),
                () -> assertTrue(response.body().contains("DEFAULT:'標準'")),
                () -> assertTrue(response.body().contains("EAGER_SLUGGISH:'ブンブン丸'")),
                () -> assertFalse(response.body().contains("EAGER_SLUGGISH:'長打重視'")),
                () -> assertTrue(response.body().contains("EAGER_STEAL:'盗塁重視'")),
                () -> assertTrue(response.body().contains("EAGER_BUNT:'バント重視'")),
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
                () -> assertTrue(response.body().contains("未ログイン")),
                () ->
                        assertFalse(
                                response.body().contains("href=\"/oauth2/authorization/google\"")),
                () -> assertContainsPattern(response.body(), "盗塁判定\\s*→\\s*バント判定\\s*→\\s*通常打撃"),
                () -> assertTrue(response.body().contains("各選手の入力項目")),
                () -> assertTrue(response.body().contains("出塁率")),
                () -> assertTrue(response.body().contains("長打率")),
                () -> assertTrue(response.body().contains("バント成功率")),
                () -> assertTrue(response.body().contains("<strong>0.00〜0.95</strong>")),
                () -> assertTrue(response.body().contains("盗塁成功率")),
                () -> assertTrue(response.body().contains("バント・盗塁のオン／オフ")),
                () -> assertTrue(response.body().contains("性格による違い")),
                () -> assertTrue(response.body().contains("標準")),
                () -> assertTrue(response.body().contains("盗塁は通常の頻度、バントは無死のときに試みます")),
                () -> assertTrue(response.body().contains("ブンブン丸")),
                () -> assertTrue(response.body().contains("標準より本塁打の割合が増える")),
                () -> assertTrue(response.body().contains("盗塁重視")),
                () -> assertTrue(response.body().contains("標準より一塁・二塁走者の盗塁を試みやすくなります")),
                () -> assertTrue(response.body().contains("バント重視")),
                () -> assertTrue(response.body().contains("一死でもバントを試みます")),
                () -> assertTrue(response.body().contains("平均得点")),
                () -> assertTrue(response.body().contains("盗塁死となり、アウトが一つ増えます")),
                () -> assertTrue(response.body().contains("満塁で四球になると押し出しで1点入ります")),
                () -> assertTrue(response.body().contains("進塁バントとスクイズの成功・失敗をそれぞれ色分け")),
                () -> assertTrue(response.body().contains("二盗成功・三盗成功・盗塁失敗を色分け")),
                () -> assertTrue(response.body().contains("打順を組み立てる")),
                () -> assertTrue(response.body().contains("--cyan: #25d9ff")),
                () -> assertTrue(response.body().contains("--pink: #ff4da6")),
                () -> assertTrue(response.body().contains("radial-gradient(circle at 15% 10%,")));
    }

    @Test
    @DisplayName("ログイン画面へアクセスするとGoogleログインの画面がHTMLで表示される")
    void rendersLoginPage() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
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
                () -> assertTrue(response.body().contains("ログイン")),
                () -> assertTrue(response.body().contains("Googleログインは現在利用できません")),
                () -> assertTrue(response.body().contains("トップへ戻る")));
    }
}
