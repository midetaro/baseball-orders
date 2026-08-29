package com.example.baseballorders.simulator.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.usecase.SimulateGameUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

class SqsSimulationSchedulerTest {

    @Test
    @DisplayName("ポーリング処理には60秒の固定遅延が設定されている")
    void pollsEveryMinute() throws NoSuchMethodException {
        // given
        var pollMethod = SqsSimulationScheduler.class.getMethod("poll");

        // when
        Scheduled result = pollMethod.getAnnotation(Scheduled.class);

        // then
        assertAll(() -> assertEquals(60_000L, result.fixedDelay()));
    }

    @Test
    @DisplayName("受信したメッセージを打順へ変換すると試合を実行してメッセージを削除する")
    void mapsReceivedMessageAndPassesLineUpToUseCase() throws Exception {
        // given
        SqsClient sqsClient = mock(SqsClient.class);
        SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
        AtBatBehavior atBatBehavior = (hitAverage, sluggish) -> BattingResult.OUT;
        StealStrategy stealStrategy = new FixedStealStrategy();
        LineUpMapper mapper = new LineUpMapper(atBatBehavior, stealStrategy);
        ObjectMapper objectMapper = new ObjectMapper();
        List<PlayerData> players =
                IntStream.rangeClosed(1, 9)
                        .mapToObj(number -> new PlayerData("player-" + number, 0.3f, 0.4f))
                        .toList();
        String body =
                objectMapper.writeValueAsString(
                        new SimulationRequest("game-1", "result-url", players));
        Message message = Message.builder().body(body).receiptHandle("receipt-1").build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        SqsSimulationScheduler scheduler =
                new SqsSimulationScheduler(sqsClient, objectMapper, useCase, mapper, "request-url");
        var lineUpCaptor =
                ArgumentCaptor.forClass(
                        com.example.baseballorders.simulator.domain.model.player.LineUpEntity
                                .class);

        // when
        scheduler.poll();

        // then
        assertAll(
                () -> verify(useCase).simulateGame(lineUpCaptor.capture()),
                () -> assertEquals(9, lineUpCaptor.getValue().getBatterEntities().size()),
                () -> verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class)));
    }

    private static final class FixedStealStrategy implements StealStrategy {

        @Override
        public StealResult runToDouble() {
            return StealResult.NOT_TRY;
        }

        @Override
        public StealResult runToTriple() {
            return StealResult.NOT_TRY;
        }
    }
}
