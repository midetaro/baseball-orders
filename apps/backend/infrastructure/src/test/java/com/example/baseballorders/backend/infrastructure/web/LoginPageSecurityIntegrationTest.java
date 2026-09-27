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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 実物: HTTPサーバー、Spring Security、LoginPageController、Thymeleaf。 モック: SqsTemplate、
 * ClientRegistrationRepository。 担保する疎通: HTTP GET /login -> Spring Security -> LoginPageController
 * -> Thymeleaf HTML応答。 担保しないもの: Googleへの認可リダイレクト、OIDC認証、SQS通信。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.cloud.aws.sqs.enabled=false",
            "baseball-orders.security.google.client-id=test-client-id",
            "baseball-orders.security.google.client-secret=test-client-secret"
        })
class LoginPageSecurityIntegrationTest {

    @MockitoBean private SqsTemplate sqsTemplate;
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;

    @LocalServerPort private int port;

    @Test
    @DisplayName("Google OAuthが有効でもログイン画面はカスタムHTMLを表示する")
    void rendersCustomLoginPageWhenGoogleOauthIsEnabled() throws Exception {
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
                () -> assertTrue(response.body().contains("打順監督にログイン")),
                () -> assertTrue(response.body().contains("Googleでログイン")),
                () -> assertTrue(response.body().contains("/oauth2/authorization/google")));
    }
}
