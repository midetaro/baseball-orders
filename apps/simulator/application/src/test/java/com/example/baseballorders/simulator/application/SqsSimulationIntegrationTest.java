package com.example.baseballorders.simulator.application;

import com.example.baseballorders.simulator.domain.model.behavior.NowayStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.ShortDistanceAtBatBehavior;
import com.example.baseballorders.simulator.domain.model.player.Batter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticmq.rest.sqs.SQSRestServer;
import org.elasticmq.rest.sqs.SQSRestServerBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class SqsSimulationIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("SQSから選手データを受信して試合結果をSQSへ返却する")
    @Test
    void receivesPlayersFromSqsAndReturnsSimulationResultToSqs() throws Exception {
        SQSRestServer sqsServer = SQSRestServerBuilder.withDynamicPort().start();
        try (var sqs = sqsClientFor(sqsServer);
             var listener = Executors.newSingleThreadExecutor()) {
            String requestQueueUrl = sqs.createQueue(request -> request.queueName("simulation-requests")).queueUrl();
            String resultQueueUrl = sqs.createQueue(request -> request.queueName("simulation-results")).queueUrl();
            var useCase = new SimulateGameUseCase(Map.of(
                    "shortDistanceAtBat", new ShortDistanceAtBatBehavior()
            ));

            var listening = listener.submit(() -> {
                Message message = sqs.receiveMessage(request -> request
                                .queueUrl(requestQueueUrl)
                                .waitTimeSeconds(10)
                                .maxNumberOfMessages(1))
                        .messages().getFirst();
                SimulationRequest request = objectMapper.readValue(message.body(), SimulationRequest.class);
                var atBatBehavior = new ShortDistanceAtBatBehavior();
                var stealStrategy = new NowayStealBehavior();
                List<Batter> batters = request.players().stream()
                        .map(player -> new Batter(
                                player.name(), player.hitAverage(), player.slugging(), atBatBehavior, stealStrategy))
                        .toList();
                var game = useCase.simulateGame(batters);
                String resultBody = objectMapper.writeValueAsString(
                        new SimulationResult(request.gameId(), game.getInning(), game.getTotalScore(), game.isGameOver()));
                sqs.sendMessage(send -> send
                        .queueUrl(request.resultQueueUrl())
                        .messageBody(resultBody));
                sqs.deleteMessage(delete -> delete
                        .queueUrl(requestQueueUrl)
                        .receiptHandle(message.receiptHandle()));
                return null;
            });

            var players = java.util.stream.IntStream.rangeClosed(1, 9)
                    .mapToObj(number -> new PlayerData("player-" + number, 0.0f, 0.0f))
                    .toList();
            String requestBody = objectMapper.writeValueAsString(
                    new SimulationRequest("game-1", resultQueueUrl, players));
            sqs.sendMessage(request -> request
                    .queueUrl(requestQueueUrl)
                    .messageBody(requestBody));

            Message returnedMessage = receiveOne(sqs, resultQueueUrl);
            SimulationResult result = objectMapper.readValue(returnedMessage.body(), SimulationResult.class);
            listening.get();

            assertAll(
                    () -> assertEquals("game-1", result.gameId()),
                    () -> assertEquals(9, result.inning()),
                    () -> assertEquals(0, result.totalScore()),
                    () -> assertTrue(result.gameOver())
            );
        } finally {
            sqsServer.stopAndWait();
        }
    }

    private static SqsClient sqsClientFor(SQSRestServer server) {
        return SqsClient.builder()
                .endpointOverride(URI.create("http://localhost:" + server.waitUntilStarted().localAddress().getPort()))
                .region(Region.AP_NORTHEAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
    }

    private static Message receiveOne(SqsClient sqs, String queueUrl) {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            var messages = sqs.receiveMessage(request -> request
                    .queueUrl(queueUrl)
                    .waitTimeSeconds(1)
                    .maxNumberOfMessages(1)).messages();
            if (!messages.isEmpty()) {
                return messages.getFirst();
            }
        }
        throw new AssertionError("simulation result was not returned to SQS");
    }

    private record SimulationRequest(String gameId, String resultQueueUrl, List<PlayerData> players) {
    }

    private record PlayerData(String name, float hitAverage, float slugging) {
    }

    private record SimulationResult(String gameId, long inning, long totalScore, boolean gameOver) {
    }
}
