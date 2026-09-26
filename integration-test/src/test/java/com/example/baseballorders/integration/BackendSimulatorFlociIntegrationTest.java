package com.example.baseballorders.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.backend.BackendApplication;
import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.application.usecase.SimulationRunMode;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.game.BaseStateFactory;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilitiesBuilder;
import com.example.baseballorders.simulator.domain.rule.HittingDistributionBuilder;
import com.example.baseballorders.simulator.domain.rule.RunnerAdvanceProbabilitiesBuilder;
import com.example.baseballorders.simulator.domain.rule.SimulationRules;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesBuilder;
import com.example.baseballorders.simulator.domain.rule.StealAttemptRatesBuilder;
import com.example.baseballorders.simulator.infrastructure.config.SimulationPitcherProperties;
import com.example.baseballorders.simulator.infrastructure.config.SimulationPitcherProperties.Multipliers;
import com.example.baseballorders.simulator.domain.statistics.ScoreStatistics;
import com.example.baseballorders.simulator.domain.statistics.ScoreStatisticsBuilder;
import com.example.baseballorders.simulator.infrastructure.messaging.LineUpMapper;
import com.example.baseballorders.simulator.infrastructure.messaging.SqsSimulationScheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.floci.testcontainers.FlociContainer;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * 実物: backendのHTTPサーバー、Controller、Coordinator、H2、要求Publisher、結果SQS Listener、
 * WaitingResultRegistry、simulatorのSqsSimulationScheduler、LineUpMapper、SimulateGameUseCase、Floci SQS。
 * モック: AWS SQSをFlociに置換。
 * 担保する疎通: HTTP POST /simulations -> backend -> 要求SQS -> simulatorの受信・試合計算
 * -> 結果SQS -> backendの自動Listener -> WaitingResultRegistry -> HTTP応答。
 * 担保しないもの: AWS実環境のIAM・ネットワーク、乱数戦略の統計的正当性、定期実行の間隔、異常系・再配信。
 * simulatorのpollはテストから起動し、SQS受信・結果送信・要求削除は本番実装を使用する。
 */
class BackendSimulatorFlociIntegrationTest {

    // Springが application.yml から束縛する確率設定を、テストでは明示値で組み立てて渡す。
    private static final SimulationRules SIMULATION_RULES = SimulationRulesBuilder.simulationRules()
            .batting(BattingProbabilitiesBuilder.battingProbabilities()
                    .walkProbability(0.05f)
                    .strikeoutProbabilityWhenNotOnBase(0.25f)
                    .build())
            .middleDistanceHitting(HittingDistributionBuilder.hittingDistribution()
                    .doubleDivisor(6)
                    .tripleDivisor(6)
                    .homeRunDivisor(6)
                    .singleReductionDivisor(2)
                    .build())
            .longDistanceHitting(HittingDistributionBuilder.hittingDistribution()
                    .doubleDivisor(8)
                    .tripleDivisor(8)
                    .homeRunDivisor(2)
                    .singleReductionDivisor(1)
                    .build())
            .standardSteal(StealAttemptRatesBuilder.stealAttemptRates()
                    .toDoubleAttemptRate(0.2f)
                    .toTripleAttemptRate(0.05f)
                    .build())
            .eagerSteal(StealAttemptRatesBuilder.stealAttemptRates()
                    .toDoubleAttemptRate(0.3f)
                    .toTripleAttemptRate(0.15f)
                    .build())
            .runnerAdvance(RunnerAdvanceProbabilitiesBuilder.runnerAdvanceProbabilities()
                    .fromFirstProbability(0.2f)
                    .fromSecondProbability(0.2f)
                    .fromThirdProbability(0.1f)
                    .build())
            .build();

    private static final BehaviorStrategies STRATEGIES = new BehaviorStrategies(SIMULATION_RULES);

    private static final SimulationPitcherProperties PITCHER_PROPERTIES =
            new SimulationPitcherProperties(new Multipliers(1.0f, 1.0f, 1.0f));


    private static HttpClient authenticatedClient(int port) throws Exception {
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
        assertTrue(csrfMatcher.find(), "ログインページはCSRFトークンを返す");
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
        assertEquals(302, login.statusCode(), "テスト用ローカルアカウントでログインする");
        return client;
    }

    /**
     * 実物: backend HTTPサーバー・Controller・Coordinator・H2・SQS Publisher/Listener、
     * simulatorのLineUpMapper・SimulateGameUseCase・SqsSimulationScheduler、Floci SQS。
     * モック: AWS SQSをFlociに置換。
     * 担保する疎通: HTTP POST -> 要求SQS -> 実試合 -> 結果SQS -> backend Listener -> HTTP応答。
     * 担保しないもの: AWS実環境、乱数戦略の統計的正当性、ブラウザ描画。
     */
    @Test
    @DisplayName("HTTP要求をsimulatorがFloci経由で処理しbackendが結果SQSを受信して同じ相関IDで応答する")
    void completesBackendRequestAfterSimulatorProcessesIt() throws Exception {
        runScenario(new SimulateGameUseCase(1, new BaseStateFactory(SIMULATION_RULES.runnerAdvance())), null);
    }

    /**
     * 実物: backend HTTPサーバー・Coordinator・H2・SQS Publisher/Listener、simulatorの
     * LineUpMapper・SqsSimulationSchedulerとJSONシリアライザ、Floci SQS。
     * モック: AWS SQSをFlociに置換。乱数を使うSimulateGameUseCaseのみ固定統計を返すfakeに置換。
     * 担保する疎通: HTTP POST -> 要求SQS -> simulator結果マッピング・JSON -> 結果SQS
     * -> backend自動Listener -> HTTP応答に得点統計、安打内訳、プレー内容統計がそのまま届く。
     * 担保しないもの: 実試合による安打・プレー内容統計の発生条件、AWS実環境、ブラウザ描画。
     */
    @Test
    @DisplayName("得点・安打内訳・プレー内容統計がsimulatorの結果SQSからbackend HTTPまで保持される")
    void carriesDetailedTacticalStatisticsAcrossQueues() throws Exception {
        var expected = ScoreStatisticsBuilder.scoreStatistics()
                .averageScore(4)
                .medianScore(4)
                .maximumScore(4)
                .gameCount(1)
                .scoreDistribution(Map.of(4, 1))
                .hitCount(100)
                .singleHitCount(50)
                .doubleHitCount(25)
                .tripleHitCount(15)
                .homeRunCount(10)
                .soloHomeRunCount(1)
                .twoRunHomeRunCount(2)
                .threeRunHomeRunCount(3)
                .grandSlamCount(4)
                .buntCount(24)
                .stealCount(52)
                .buntFailureCount(36)
                .stealFailureCount(40)
                .advancingBuntCount(11)
                .squeezeBuntCount(13)
                .advancingBuntFailureCount(17)
                .squeezeBuntFailureCount(19)
                .stealToSecondCount(23)
                .stealToThirdCount(29)
                .build();
        var fixedUseCase = new SimulateGameUseCase(1, new BaseStateFactory(SIMULATION_RULES.runnerAdvance())) {
            @Override
            public SimulationResult invoke(LineUpEntity lineup, SimulationRunMode mode) {
                assertEquals(9, lineup.getBatterEntities().size());
                assertEquals(SimulationRunMode.LARGE_SCALE_RUN, mode);
                return new SimulationResult(expected);
            }
        };
        runScenario(fixedUseCase, expected);
    }

    private void runScenario(SimulateGameUseCase useCase, ScoreStatistics expected)
            throws Exception {
        // given
        var suffix = UUID.randomUUID().toString();
        var requestQueue = "simulation-request-" + suffix;
        var resultQueue = "simulation-result-" + suffix;
        try (var floci = new FlociContainer().withSqsConfig(c -> c.enabled(true)).withDockerSocket(false)) {
            floci.start();
            try (var sqs = SqsClient.builder()
                    .endpointOverride(URI.create(floci.getEndpoint()))
                    .region(Region.of(floci.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(floci.getAccessKey(), floci.getSecretKey())))
                    .build()) {
                var requestUrl = sqs.createQueue(r -> r.queueName(requestQueue)).queueUrl();
                sqs.createQueue(r -> r.queueName(resultQueue));
                try (var backend = new SpringApplicationBuilder(BackendApplication.class).run(
                                "--spring.profiles.active=integration",
                                "--server.port=0",
                                "--spring.cloud.aws.region.static=" + floci.getRegion(),
                                "--spring.cloud.aws.credentials.access-key=" + floci.getAccessKey(),
                                "--spring.cloud.aws.credentials.secret-key=" + floci.getSecretKey(),
                                "--spring.cloud.aws.sqs.endpoint=" + floci.getEndpoint(),
                                "--spring.datasource.url=jdbc:h2:mem:" + suffix,
                                "--simulation.sqs.request-queue-name=" + requestQueue,
                                "--simulation.sqs.result-queue-name=" + resultQueue);
                        var http = authenticatedClient(
                                ((WebServerApplicationContext) backend).getWebServer().getPort())) {
                    var port = ((WebServerApplicationContext) backend).getWebServer().getPort();
                    var registry = backend.getBean(WaitingResultRegistry.class);
                    var mapper = new ObjectMapper();
                    var lineupMapper = new LineUpMapper(
                            STRATEGIES.middleDistanceHittingStrategy(),
                            STRATEGIES.noSteal(),
                            STRATEGIES.standardBunt(),
                            STRATEGIES,
                            PITCHER_PROPERTIES);
                    var simulator = new SqsSimulationScheduler(
                            sqs, mapper, useCase, lineupMapper, requestQueue, resultQueue, 10, 10);
                    var request = HttpRequest.newBuilder(
                                    URI.create("http://localhost:" + port + "/simulations"))
                            .timeout(Duration.ofSeconds(30))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("""
                                    [{"hit_average":0.300,"sluggish":0.400,"bunt_success_rate":0.700,"steal_success_rate":0.700,"bunt_enabled":false,"steal_enabled":false},
                                     {"hit_average":0.300,"sluggish":0.400,"bunt_success_rate":0.700,"steal_success_rate":0.700,"bunt_enabled":false,"steal_enabled":false},
                                     {"hit_average":0.300,"sluggish":0.400,"bunt_success_rate":0.700,"steal_success_rate":0.700,"bunt_enabled":false,"steal_enabled":false},
                                     {"hit_average":0.300,"sluggish":0.400,"bunt_success_rate":0.700,"steal_success_rate":0.700,"bunt_enabled":false,"steal_enabled":false},
                                     {"hit_average":0.300,"sluggish":0.400,"bunt_success_rate":0.700,"steal_success_rate":0.700,"bunt_enabled":false,"steal_enabled":false},
                                     {"hit_average":0.300,"sluggish":0.400,"bunt_success_rate":0.700,"steal_success_rate":0.700,"bunt_enabled":false,"steal_enabled":false},
                                     {"hit_average":0.300,"sluggish":0.400,"bunt_success_rate":0.700,"steal_success_rate":0.700,"bunt_enabled":false,"steal_enabled":false},
                                     {"hit_average":0.300,"sluggish":0.400,"bunt_success_rate":0.700,"steal_success_rate":0.700,"bunt_enabled":false,"steal_enabled":false},
                                     {"hit_average":0.300,"sluggish":0.400,"bunt_success_rate":0.700,"steal_success_rate":0.700,"bunt_enabled":false,"steal_enabled":false}]
                                    """))
                            .build();

                    // when
                    var responseFuture = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                    // 要求を観測して相関IDを保存し、可視性を戻して本番simulatorに処理させる。
                    var messages = sqs
                            .receiveMessage(r -> r.queueUrl(requestUrl)
                            .waitTimeSeconds(10).maxNumberOfMessages(1)).messages();

                    assertAll(() -> assertEquals(1, messages.size(), "backendが要求SQSへ送信する"));
                    var message = messages.getFirst();
                    var wireRequest = mapper.readValue(message.body(), SimulationRequestMessage.class);
                    assertAll(() -> assertFalse(responseFuture.isDone(), "結果受信までHTTPは待機する"));
                    sqs.changeMessageVisibility(r -> r.queueUrl(requestUrl)
                            .receiptHandle(message.receiptHandle()).visibilityTimeout(0));
                    simulator.poll();
                    var response = responseFuture.get(10, TimeUnit.SECONDS);
                    JsonNode body = mapper.readTree(response.body());

                    // then
                    assertAll(
                            () -> assertEquals(200, response.statusCode(), response.body()),
                            () -> assertNotNull(wireRequest.simulationId()),
                            () -> assertEquals(wireRequest.simulationId().toString(), body.path("simulationId").asText()),
                            () -> assertEquals(9, wireRequest.players().size()),
                            () -> assertEquals(1, body.path("statistics").path("gameCount").asInt(-1)),
                            () -> assertEquals(0, registry.pendingCount())
                    );
                    if (expected != null) {
                        var statistics = body.path("statistics");
                        assertAll(
                                () -> assertEquals(expected.averageScore(), statistics.path("averageScore").asDouble(-1)),
                                () -> assertEquals(expected.medianScore(), statistics.path("medianScore").asDouble(-1)),
                                () -> assertEquals(expected.maximumScore(), statistics.path("maximumScore").asInt(-1)),
                                () -> assertEquals(expected.gameCount(), statistics.path("gameCount").asInt(-1)),
                                () -> assertEquals(expected.scoreDistribution().get(4).intValue(),
                                        statistics.path("scoreDistribution").path("4").asInt(-1)),
                                () -> assertEquals(expected.hitCount(), statistics.path("hitCount").asInt(-1)),
                                () -> assertEquals(expected.singleHitCount(), statistics.path("singleHitCount").asInt(-1)),
                                () -> assertEquals(expected.doubleHitCount(), statistics.path("doubleHitCount").asInt(-1)),
                                () -> assertEquals(expected.tripleHitCount(), statistics.path("tripleHitCount").asInt(-1)),
                                () -> assertEquals(expected.homeRunCount(), statistics.path("homeRunCount").asInt(-1)),
                                () -> assertEquals(expected.soloHomeRunCount(), statistics.path("soloHomeRunCount").asInt(-1)),
                                () -> assertEquals(expected.twoRunHomeRunCount(), statistics.path("twoRunHomeRunCount").asInt(-1)),
                                () -> assertEquals(expected.threeRunHomeRunCount(), statistics.path("threeRunHomeRunCount").asInt(-1)),
                                () -> assertEquals(expected.grandSlamCount(), statistics.path("grandSlamCount").asInt(-1)),
                                () -> assertEquals(expected.buntCount(), statistics.path("buntCount").asInt(-1)),
                                () -> assertEquals(expected.stealCount(), statistics.path("stealCount").asInt(-1)),
                                () -> assertEquals(expected.buntFailureCount(), statistics.path("buntFailureCount").asInt(-1)),
                                () -> assertEquals(expected.stealFailureCount(), statistics.path("stealFailureCount").asInt(-1)),
                                () -> assertEquals(expected.advancingBuntCount(), statistics.path("advancingBuntCount").asInt(-1)),
                                () -> assertEquals(expected.squeezeBuntCount(), statistics.path("squeezeBuntCount").asInt(-1)),
                                () -> assertEquals(expected.advancingBuntFailureCount(), statistics.path("advancingBuntFailureCount").asInt(-1)),
                                () -> assertEquals(expected.squeezeBuntFailureCount(), statistics.path("squeezeBuntFailureCount").asInt(-1)),
                                () -> assertEquals(expected.stealToSecondCount(), statistics.path("stealToSecondCount").asInt(-1)),
                                () -> assertEquals(expected.stealToThirdCount(), statistics.path("stealToThirdCount").asInt(-1)));
                    }
                }
            }
        }
    }
}
