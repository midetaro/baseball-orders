package com.example.baseballorders.simulator.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.baseballorders.simulator.application.LineUpMapper;
import com.example.baseballorders.simulator.application.PlayerData;
import com.example.baseballorders.simulator.application.SimulateGameUseCase;
import com.example.baseballorders.simulator.application.SimulationRequest;
import com.example.baseballorders.simulator.application.dto.SimulationResponse;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

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
    @DisplayName("受信した試合を実行すると結果を指定されたSQSへ送信して元メッセージを削除する")
    void sendsSimulationResponseAndDeletesReceivedMessage() throws Exception {
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
        var simulationResponse = new SimulationResponse(5, 4);
        when(useCase.simulateGame(any(LineUpEntity.class))).thenReturn(simulationResponse);
        SqsSimulationScheduler scheduler =
                new SqsSimulationScheduler(sqsClient, objectMapper, useCase, mapper, "request-url");
        var lineUpCaptor = ArgumentCaptor.forClass(LineUpEntity.class);
        var sendMessageCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        var ordered = inOrder(useCase, sqsClient);

        // when
        scheduler.poll();

        // then
        assertAll(
                () -> ordered.verify(useCase).simulateGame(lineUpCaptor.capture()),
                () -> ordered.verify(sqsClient).sendMessage(sendMessageCaptor.capture()),
                () -> ordered.verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class)),
                () -> assertEquals(9, lineUpCaptor.getValue().getBatterEntities().size()),
                () -> assertEquals("result-url", sendMessageCaptor.getValue().queueUrl()),
                () ->
                        assertEquals(
                                objectMapper.writeValueAsString(simulationResponse),
                                sendMessageCaptor.getValue().messageBody()));
    }

    @Test
    @DisplayName("シミュレーション結果をJSONへ変換できない場合はSQSへ送信せず元メッセージも削除しない")
    void doesNotSendOrDeleteWhenResponseSerializationFails() throws Exception {
        // given
        SqsClient sqsClient = mock(SqsClient.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
        LineUpMapper mapper = mock(LineUpMapper.class);
        var request = new SimulationRequest("game-1", "result-url", List.of());
        var response = new SimulationResponse(5, 4);
        Message message = Message.builder().body("request-body").receiptHandle("receipt-1").build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(objectMapper.readValue("request-body", SimulationRequest.class)).thenReturn(request);
        when(useCase.simulateGame(any())).thenReturn(response);
        when(objectMapper.writeValueAsString(response))
                .thenThrow(new JsonProcessingException("serialization failed") {});
        SqsSimulationScheduler scheduler =
                new SqsSimulationScheduler(sqsClient, objectMapper, useCase, mapper, "request-url");

        // when
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, scheduler::poll);

        // then
        assertAll(
                () ->
                        assertEquals(
                                "Failed to serialize an SQS simulation response",
                                exception.getMessage()),
                () -> verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class)),
                () -> verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class)));
    }

    @Test
    @DisplayName("結果SQSへの送信に失敗した場合は元メッセージを削除しない")
    void doesNotDeleteReceivedMessageWhenResponseSendFails() throws Exception {
        // given
        SqsClient sqsClient = mock(SqsClient.class);
        SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
        LineUpMapper mapper = mock(LineUpMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        var request = new SimulationRequest("game-1", "result-url", List.of());
        var response = new SimulationResponse(5, 4);
        Message message =
                Message.builder()
                        .body(objectMapper.writeValueAsString(request))
                        .receiptHandle("receipt-1")
                        .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(useCase.simulateGame(any())).thenReturn(response);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(SqsException.builder().message("send failed").build());
        SqsSimulationScheduler scheduler =
                new SqsSimulationScheduler(sqsClient, objectMapper, useCase, mapper, "request-url");

        // when
        SqsException exception = assertThrows(SqsException.class, scheduler::poll);

        // then
        assertAll(
                () -> assertEquals("send failed", exception.getMessage()),
                () -> verify(sqsClient).sendMessage(any(SendMessageRequest.class)),
                () -> verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class)));
    }

    @Test
    @DisplayName("受信メッセージをJSONから変換できない場合は試合を実行せず元メッセージも削除しない")
    void doesNotSimulateOrDeleteWhenRequestDeserializationFails() {
        // given
        SqsClient sqsClient = mock(SqsClient.class);
        SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
        LineUpMapper mapper = mock(LineUpMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        Message message = Message.builder().body("{").receiptHandle("receipt-1").build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        SqsSimulationScheduler scheduler =
                new SqsSimulationScheduler(sqsClient, objectMapper, useCase, mapper, "request-url");

        // when
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, scheduler::poll);

        // then
        assertAll(
                () ->
                        assertEquals(
                                "Failed to deserialize an SQS simulation request",
                                exception.getMessage()),
                () -> verifyNoInteractions(useCase),
                () -> verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class)),
                () -> verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class)));
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
