package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 実物: HTTPサーバー、SimulationPageController、SimulationGuidePageController、Thymeleaf、静的リソース配信、
 * TypeScriptからビルド時に生成した画面スクリプト（ES module）。 モック: SqsTemplate。 担保する疎通: ログインなしのHTTP GET / ->
 * SimulationPageController -> 打順組み替え画面のThymeleaf HTML応答。HTTP GET /large-scale および /single-game ->
 * SimulationPageController -> Thymeleaf HTML応答、HTTP GET /simulation-guide ->
 * SimulationGuidePageController -> Thymeleaf
 * HTML応答も担保する。4画面すべてで共通の左メニュー断片（fragments/site-menu.html）が描画され、他画面への導線がメニューにだけあることも担保する。分離したHTTP
 * GET /css/simulation.css、/css/single-game.css、/css/batting-order.css -> 静的リソース配信 ->
 * CSS応答と、各画面のHTML応答 -> 画面の入口モジュール /js/pages/*.js -> import先のモジュール -> 静的リソース配信 ->
 * JavaScript応答（ブラウザが読み込むモジュールをすべて取得できること）も担保する。担保しないもの:
 * SQSへのシミュレーション要求送信と結果受信、画面スクリプトの振る舞い（src/test/js のNodeテストで検査する）、入力値・ドラッグ操作・メニュー開閉のブラウザ操作。
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

    private static final Pattern MODULE_SCRIPT =
            Pattern.compile("<script src=\"(/js/pages/[a-z-]+\\.js)\" type=\"module\"></script>");
    private static final Pattern RELATIVE_IMPORT = Pattern.compile("from '(\\.{1,2}/[^']+)'");

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    /** 画面の入口モジュールからimportを辿り、ブラウザが読み込むモジュールをすべて取得する。 */
    private Map<String, HttpResponse<String>> fetchModuleGraph(HttpClient client, String entry)
            throws Exception {
        Map<String, HttpResponse<String>> modules = new LinkedHashMap<>();
        Deque<String> pending = new ArrayDeque<>(List.of(entry));
        while (!pending.isEmpty()) {
            var path = pending.pop();
            if (modules.containsKey(path)) {
                continue;
            }
            var response = get(client, path);
            modules.put(path, response);
            var imports = RELATIVE_IMPORT.matcher(response.body());
            while (imports.find()) {
                pending.push(URI.create(path).resolve(imports.group(1)).getPath());
            }
        }
        return modules;
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

    @ParameterizedTest
    @ValueSource(strings = {"/", "/large-scale", "/single-game", "/simulation-guide"})
    @DisplayName("どの画面も他画面への導線は閉じた左メニューだけに置き、現在の画面をメニューで示す")
    void rendersSiteMenuAsOnlyNavigation(String route) throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + route))
                        .GET()
                        .build();

        // when
        HttpResponse<String> response;
        try (var client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // then
        var body = response.body();
        var menuStart = body.indexOf("<nav aria-label=\"画面メニュー\"");
        var menuEnd = body.indexOf("</nav>", menuStart);
        var menu = menuStart < 0 ? "" : body.substring(menuStart, menuEnd);
        var outsideMenu =
                menuStart < 0 ? body : body.substring(0, menuStart) + body.substring(menuEnd);
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () ->
                        assertContainsPattern(
                                body, "<nav[^>]*class=\"site-menu\" hidden id=\"site-menu\">"),
                () ->
                        assertContainsPattern(
                                body,
                                "<button[^>]*aria-controls=\"site-menu\""
                                        + " aria-expanded=\"false\"[^>]*id=\"menu-toggle\""),
                () -> assertContainsPattern(menu, "href=\"/\"[^>]*>打順組み替え</a>"),
                () -> assertContainsPattern(menu, "href=\"/large-scale\"[^>]*>大規模実行</a>"),
                () -> assertContainsPattern(menu, "href=\"/single-game\"[^>]*>1試合実行</a>"),
                () ->
                        assertContainsPattern(
                                menu, "href=\"/simulation-guide\"[^>]*>シミュレーションの仕組み</a>"),
                () ->
                        assertContainsPattern(
                                menu,
                                "<a (?=[^>]*aria-current=\"page\")(?=[^>]*href=\""
                                        + Pattern.quote(route)
                                        + "\")[^>]*>"),
                () -> assertEquals(1, menu.split("aria-current=", -1).length - 1),
                () ->
                        assertFalse(
                                Pattern.compile(
                                                "<a [^>]*href=\"/(large-scale|single-game|simulation-guide)?\"")
                                        .matcher(outsideMenu)
                                        .find()),
                () ->
                        assertTrue(
                                body.contains(
                                        "<link rel=\"stylesheet\" href=\"/css/site-menu.css\">")),
                () -> assertContainsPattern(body, MODULE_SCRIPT.pattern()));
    }

    @Test
    @DisplayName("トップ画面へアクセスすると打順組み替え画面と左メニューがHTMLで表示される")
    void rendersBattingOrderPageAtRoot() throws Exception {
        // given
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();

        // when
        HttpResponse<String> response;
        HttpResponse<String> cssResponse;
        try (var client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            cssResponse =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/css/batting-order.css"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("<h2 id=\"order-heading\">打順組み替え</h2>")),
                () -> assertTrue(response.body().contains("data-lineup-mode=\"reorder\"")),
                () ->
                        assertContainsPattern(
                                response.body(), "<option[^>]*value=\"STRONG\"[^>]*>強</option>"),
                () ->
                        assertContainsPattern(
                                response.body(), "<option[^>]*value=\"AVERAGE\"[^>]*>並</option>"),
                () ->
                        assertContainsPattern(
                                response.body(), "<option[^>]*value=\"WEAK\"[^>]*>弱</option>"),
                () -> assertTrue(response.body().contains("data-team=\"STRONG\"")),
                () -> assertTrue(response.body().contains("data-hit-average=\"0.")),
                () -> assertTrue(response.body().contains("data-personality=")),
                () -> assertTrue(response.body().contains("id=\"average-hit-average\"")),
                () -> assertFalse(response.body().contains("id=\"reset-all-personalities\"")),
                () -> assertTrue(response.body().contains("id=\"menu-toggle\"")),
                () -> assertTrue(response.body().contains("aria-expanded=\"false\"")),
                () ->
                        assertContainsPattern(
                                response.body(),
                                "<nav[^>]*class=\"site-menu\" hidden id=\"site-menu\">"),
                () -> assertTrue(response.body().contains("href=\"/large-scale\"")),
                () -> assertTrue(response.body().contains("href=\"/single-game\"")),
                () -> assertTrue(response.body().contains("href=\"/simulation-guide\"")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains(
                                                "<script src=\"/js/pages/batting-order.js\""
                                                        + " type=\"module\"></script>")),
                () -> assertTrue(response.body().contains("id=\"score-histogram\"")),
                () -> assertEquals(200, cssResponse.statusCode()),
                () -> assertTrue(cssResponse.body().contains(".drag-handle")));
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
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("打順入力")),
                () -> assertTrue(response.body().contains("<span>打率</span>")),
                () -> assertFalse(response.body().contains("<dt>出塁率</dt>")),
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
                () ->
                        assertTrue(
                                response.body()
                                        .contains("data-bunt-frame-duration-millis=\"1600\"")),
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
                                                "<script src=\"/js/pages/single-game.js\""
                                                        + " type=\"module\"></script>")));
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
        HttpResponse<String> cssResponse;
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
                                                "<script src=\"/js/pages/simulation.js\""
                                                        + " type=\"module\"></script>")),
                () -> assertTrue(response.body().contains("打率")),
                () -> assertFalse(response.body().contains("長打率")),
                () -> assertTrue(response.body().contains("SIMULATIONを実行")),
                () -> assertTrue(response.body().contains("<h2 id=\"order-heading\">打順入力</h2>")),
                () -> assertTrue(response.body().contains("id=\"toggle-all-bunt\"")),
                () -> assertTrue(response.body().contains("id=\"toggle-all-steal\"")),
                () -> assertTrue(response.body().contains("href=\"/simulation-guide\"")),
                () -> assertFalse(response.body().toLowerCase().contains("pitcher")),
                () -> assertTrue(response.body().contains("本塁打")),
                () -> assertTrue(response.body().contains("バント")),
                () -> assertTrue(response.body().contains("盗塁")),
                () -> assertTrue(response.body().contains("得点サマリー")),
                () -> assertTrue(response.body().contains("本塁打の内訳")),
                () -> assertTrue(!response.body().contains("戦術の成否")),
                () -> assertTrue(response.body().contains("id=\"share-results\"")),
                () ->
                        assertTrue(
                                cssResponse
                                        .body()
                                        .matches("(?s).*\\.order \\{.*width:\\s*max-content;.*")),
                () ->
                        assertContainsPattern(
                                cssResponse.body(),
                                "grid-template-columns:\\d+px\\s+\\d+px\\s+\\d+px\\s+\\d+px\\s+\\d+px"),
                () -> assertTrue(cssResponse.body().contains(".section-head {")),
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
                () -> assertTrue(cssResponse.body().contains("--moss: #56704a")),
                () -> assertTrue(response.body().contains("score-histogram")),
                () -> assertTrue(response.body().contains("score-distribution-axis")),
                () -> assertTrue(response.body().contains("全試合に対する割合")),
                () -> assertTrue(response.body().contains("home-run-breakdown")),
                () -> assertTrue(response.body().contains("home-run-legend")),
                () -> assertTrue(response.body().contains("本塁打なし")),
                () -> assertTrue(response.body().contains("バントの内訳")),
                () -> assertTrue(response.body().contains("盗塁の内訳")),
                () -> assertTrue(response.body().contains("id=\"bunt-count\"")),
                () -> assertTrue(response.body().contains("id=\"bunt-failure-count\"")),
                () -> assertTrue(response.body().contains("id=\"steal-count\"")),
                () -> assertTrue(response.body().contains("id=\"steal-failure-count\"")),
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
        try (var client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // then
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("id=\"reset-all-personalities\"")),
                () -> assertTrue(response.body().contains("性格")));
    }

    @ParameterizedTest
    @CsvSource({
        "/, /js/pages/batting-order.js, /js/simulation/results-view.js",
        "/large-scale, /js/pages/simulation.js, /js/simulation/results-view.js",
        "/single-game, /js/pages/single-game.js, /js/single-game/playback.js",
        "/simulation-guide, /js/pages/simulation-guide.js, /js/site-menu/site-menu.js"
    })
    @DisplayName("各画面が読み込む入口モジュールとimport先のモジュールを、すべてJavaScriptとして取得できる")
    void servesPageModuleGraph(String route, String entry, String expectedModule) throws Exception {
        // given
        var pageRequest = route;

        // when
        HttpResponse<String> page;
        Map<String, HttpResponse<String>> modules;
        try (var client = HttpClient.newHttpClient()) {
            page = get(client, pageRequest);
            var script = MODULE_SCRIPT.matcher(page.body());
            modules = script.find() ? fetchModuleGraph(client, script.group(1)) : Map.of();
        }

        // then
        var scripts = Pattern.compile("<script").matcher(page.body()).results().count();
        assertAll(
                () -> assertEquals(200, page.statusCode()),
                () -> assertEquals(1, scripts),
                () -> assertTrue(modules.containsKey(entry)),
                () -> assertTrue(modules.containsKey(expectedModule)),
                () -> assertTrue(modules.containsKey("/js/site-menu/site-menu.js")),
                () -> assertTrue(modules.containsKey("/js/shared/dom.js")),
                () ->
                        assertAll(
                                modules.entrySet().stream()
                                        .map(
                                                SimulationPageIntegrationTest
                                                        ::assertJavaScriptModule)));
    }

    /** 配信されたモジュールが、ブラウザが実行できるJavaScriptで、TypeScriptのソースをimportしていないことを検査する。 */
    private static Executable assertJavaScriptModule(
            Map.Entry<String, HttpResponse<String>> module) {
        var path = module.getKey();
        var response = module.getValue();
        return () ->
                assertAll(
                        path,
                        () -> assertEquals(200, response.statusCode()),
                        () ->
                                assertTrue(
                                        response.headers()
                                                .firstValue("Content-Type")
                                                .orElse("")
                                                .contains("javascript")),
                        () -> assertFalse(response.body().contains(".ts'")));
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
                () -> assertTrue(response.body().contains("安打数を打数で割った割合")),
                () -> assertTrue(response.body().contains("四球は打数に含めず、別に判定します")),
                () -> assertTrue(response.body().contains("本塁打・四球・三振・凡打のいずれか")),
                () -> assertFalse(response.body().contains("<dt>出塁率</dt>")),
                () -> assertTrue(response.body().contains("打率")),
                () -> assertFalse(response.body().contains("長打率")),
                () -> assertTrue(response.body().contains("単打18:二塁打2</strong>の固定比率で配分し、三塁打")),
                () -> assertFalse(response.body().contains("単打3:二塁打2")),
                () -> assertFalse(response.body().contains("バント成功率")),
                () -> assertFalse(response.body().contains("<strong>0.00〜0.95</strong>")),
                () -> assertFalse(response.body().contains("盗塁成功率")),
                () -> assertTrue(response.body().contains("バント・盗塁のオン／オフ")),
                () -> assertTrue(response.body().contains("性格による違い")),
                () -> assertFalse(response.body().contains("<dt>標準</dt>")),
                () -> assertTrue(response.body().contains("<dt>単打マン</dt>")),
                () -> assertTrue(response.body().contains("三塁打・本塁打は打ちません")),
                () -> assertTrue(response.body().contains("<dt>中距離砲</dt>")),
                () -> assertTrue(response.body().contains("単打13:二塁打3:三塁打1:本塁打3")),
                () -> assertTrue(response.body().contains("単打7:二塁打6:三塁打1:本塁打6")),
                () -> assertTrue(response.body().contains("長距離砲")),
                () -> assertTrue(response.body().contains("中距離砲より本塁打と二塁打の割合が増える一方、単打が減ります")),
                () -> assertTrue(response.body().contains("<dt>高出塁率</dt>")),
                () -> assertTrue(response.body().contains("四球の確率が<strong>10%</strong>")),
                () ->
                        assertTrue(
                                response.body()
                                        .contains("安打の配分は単打マンと同じ<strong>単打18:二塁打2</strong>")),
                () -> assertFalse(response.body().contains("安打の配分は中距離砲と同じ比率です")),
                () -> assertTrue(response.body().contains("盗塁は通常の頻度、バントは無死のときに試みます")),
                () -> assertTrue(response.body().contains("安打全体の確率は打率で決まり、変わりません")),
                () -> assertFalse(response.body().contains("アウトも増えます")),
                () -> assertTrue(response.body().contains("盗塁重視")),
                () -> assertTrue(response.body().contains("他の性格より二塁走者の盗塁を試みやすくなります")),
                () -> assertTrue(response.body().contains("<dt>バント職人</dt>")),
                () -> assertFalse(response.body().contains("バント重視")),
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
                () -> assertFalse(response.body().contains("打順を組み立てる")),
                () -> assertTrue(response.body().contains("color-scheme: light")),
                () -> assertTrue(response.body().contains("--paper: #f3eee2")),
                () -> assertTrue(response.body().contains("--moss: #56704a")),
                () -> assertTrue(response.body().contains("--clay: #bf6b45")),
                () -> assertFalse(response.body().contains("--cyan")),
                () -> assertFalse(response.body().contains("--pink")));
    }
}
