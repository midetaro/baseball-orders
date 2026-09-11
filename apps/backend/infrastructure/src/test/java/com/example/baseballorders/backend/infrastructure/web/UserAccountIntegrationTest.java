package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.backend.application.UserAccountRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 実物: HTTPサーバー、Spring Security、Thymeleaf、Flyway、H2、JPA、BCrypt。 モック: SQSを使わないためのSqsTemplate。 担保する疎通:
 * HTTPフォーム -> CSRF -> UserAccountController -> UserAccountService -> JPA -> H2。 担保しないもの:
 * ユーザー固有データの取得、パスワード再設定、外部SQS通信。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.cloud.aws.sqs.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:login-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        })
class UserAccountIntegrationTest {

    private static final Pattern CSRF =
            Pattern.compile("name=\\\"_csrf\\\" value=\\\"([^\\\"]+)\\\"");

    // ログイン機能では使用しないSqsTemplateをモックする
    @MockitoBean private SqsTemplate sqsTemplate;
    @Autowired private UserAccountRepository repository;
    @LocalServerPort private int port;

    @Test
    @DisplayName("CSRF付きフォームでアカウントを作成して正しいパスワードでログインできる")
    void registersAndLogsIn() throws Exception {
        // given
        var client =
                HttpClient.newBuilder()
                        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        var registrationPage = get(client, "/users/new");
        var csrf = csrfToken(registrationPage.body());

        // when
        var registered = postForm(client, "/users", csrf, "baseball_user", "password-123");
        var account = repository.findByUsername("baseball_user").orElseThrow();
        var loginPage = get(client, "/login");
        var loggedIn =
                postForm(
                        client,
                        "/login",
                        csrfToken(loginPage.body()),
                        "baseball_user",
                        "password-123");

        // then
        assertAll(
                () -> assertEquals(302, registered.statusCode()),
                () ->
                        assertTrue(
                                registered
                                        .headers()
                                        .firstValue("location")
                                        .orElseThrow()
                                        .endsWith("/login?registered")),
                () -> assertTrue(account.passwordHash().startsWith("$2")),
                () -> assertFalse(account.passwordHash().contains("password-123")),
                () -> assertEquals(302, loggedIn.statusCode()),
                () ->
                        assertTrue(
                                loggedIn.headers()
                                        .firstValue("location")
                                        .orElseThrow()
                                        .endsWith("/")));
    }

    @Test
    @DisplayName("存在しないユーザーは統一したログイン失敗画面へ遷移する")
    void rejectsInvalidCredentialsWithUnifiedResponse() throws Exception {
        // given
        var client =
                HttpClient.newBuilder()
                        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        var page = get(client, "/login");

        // when
        var response =
                postForm(client, "/login", csrfToken(page.body()), "unknown_user", "password-123");

        // then
        assertAll(
                () -> assertEquals(302, response.statusCode()),
                () ->
                        assertTrue(
                                response.headers()
                                        .firstValue("location")
                                        .orElseThrow()
                                        .endsWith("/login?error")));
    }

    @Test
    @DisplayName("CSRFトークンなしではアカウントを作成できない")
    void rejectsAccountCreationWithoutCsrfToken() throws Exception {
        // given
        var client = HttpClient.newHttpClient();

        // when
        var response =
                client.send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/users"))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                "username=csrf_user&password=password-123"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

        // then
        assertAll(() -> assertEquals(403, response.statusCode()));
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postForm(
            HttpClient client, String path, String csrf, String username, String password)
            throws Exception {
        var body =
                "_csrf="
                        + encode(csrf)
                        + "&username="
                        + encode(username)
                        + "&password="
                        + encode(password);
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String csrfToken(String html) {
        var matcher = CSRF.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("CSRF token is missing from form");
        }
        return matcher.group(1);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
