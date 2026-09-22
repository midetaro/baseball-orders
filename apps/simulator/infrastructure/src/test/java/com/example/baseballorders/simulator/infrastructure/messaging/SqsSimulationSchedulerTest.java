package com.example.baseballorders.simulator.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import com.example.baseballorders.simulator.application.contract.SimulationResponse;
import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StealStrategy;
import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.statistics.ScoreAccumulator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

class SqsSimulationSchedulerTest {

    private static java.util.stream.Stream<String> missingRequestValues() throws Exception {
        var objectMapper = new ObjectMapper();
        var valid =
                objectMapper.readTree(
                        """
                                {"simulation_id":"00000000-0000-0000-0000-000000000001","version":"1",
                                 "players":[{"name":"1番","hitAverage":0.3,"sluggish":0.4,"buntSuccessRate":0.7,
                                 "buntEnabled":false,"stealSuccessRate":0.8,"stealEnabled":false}]}
                                """);
        var bodies = new java.util.ArrayList<String>();
        for (String field :
                List.of(
                        "simulation_id",
                        "version",
                        "players",
                        "name",
                        "hitAverage",
                        "sluggish",
                        "buntSuccessRate",
                        "buntEnabled",
                        "stealSuccessRate",
                        "stealEnabled")) {
            for (boolean omit : List.of(true, false)) {
                com.fasterxml.jackson.databind.node.ObjectNode copy = valid.deepCopy();
                var target =
                        List.of("simulation_id", "version", "players").contains(field)
                                ? copy
                                : (com.fasterxml.jackson.databind.node.ObjectNode)
                                        copy.path("players").get(0);
                if (omit) target.remove(field);
                else target.putNull(field);
                bodies.add(copy.toString());
            }
        }
        bodies.add("null");
        var nullPlayer = valid.deepCopy();
        ((com.fasterxml.jackson.databind.node.ArrayNode) nullPlayer.path("players"))
                .set(0, objectMapper.nullNode());
        bodies.add(nullPlayer.toString());
        return bodies.stream();
    }

    private static SimulationResult simulationResult(List<SimulationResponse> responses) {
        ScoreAccumulator accumulator = new ScoreAccumulator();
        responses.forEach(
                response ->
                        accumulator.onGameCompleted(response.score(), response.gameStatistics()));
        return new SimulationResult(accumulator.toScoreStatistics());
    }

    private static void stubQueueUrls(SqsClient sqsClient) {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenAnswer(
                        invocation -> {
                            GetQueueUrlRequest request = invocation.getArgument(0);
                            return GetQueueUrlResponse.builder()
                                    .queueUrl(request.queueName().replace("-queue", "-url"))
                                    .build();
                        });
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("missingRequestValues")
    @DisplayName("SQSの必須値がnullまたは欠落なら計算も結果送信も要求削除もしない")
    void rejectsMissingRequestValues(String body) {
        // given
        var sqsClient = mock(SqsClient.class);
        var useCase = mock(SimulateGameUseCase.class);
        var mapper = mock(LineUpMapper.class);
        stubQueueUrls(sqsClient);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(
                        ReceiveMessageResponse.builder()
                                .messages(
                                        Message.builder()
                                                .body(body)
                                                .receiptHandle("receipt")
                                                .build())
                                .build());
        var scheduler =
                new SqsSimulationScheduler(
                        sqsClient,
                        new ObjectMapper(),
                        useCase,
                        mapper,
                        "request-queue",
                        "result-queue",
                        10,
                        10);

        // when
        scheduler.poll();

        // then
        assertAll(
                () -> verifyNoInteractions(useCase, mapper),
                () -> verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class)),
                () -> verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class)));
    }

    @Test
    @DisplayName("ポーリング処理の固定遅延は設定プロパティから取得する")
    void obtainsPollingDelayFromProperty() throws NoSuchMethodException {
        // given
        var pollMethod = SqsSimulationScheduler.class.getMethod("poll");

        // when
        Scheduled result = pollMethod.getAnnotation(Scheduled.class);

        // then
        assertAll(
                () ->
                        assertEquals(
                                "${simulation.sqs.poll-fixed-delay}", result.fixedDelayString()));
    }

    @Test
    @DisplayName("受信件数とロングポーリング秒数は設定プロパティから取得する")
    void obtainsReceiveOptionsFromProperties() {
        // given
        var constructors = SqsSimulationScheduler.class.getConstructors();

        // when
        var propertyNames =
                java.util.Arrays.stream(constructors)
                        .flatMap(
                                constructor -> java.util.Arrays.stream(constructor.getParameters()))
                        .map(
                                parameter ->
                                        parameter.getAnnotation(
                                                org.springframework.beans.factory.annotation.Value
                                                        .class))
                        .filter(java.util.Objects::nonNull)
                        .map(org.springframework.beans.factory.annotation.Value::value)
                        .toList();

        // then
        assertAll(
                () ->
                        assertEquals(
                                true,
                                propertyNames.contains("${simulation.sqs.max-messages-per-poll}")),
                () ->
                        assertEquals(
                                true,
                                propertyNames.contains("${simulation.sqs.long-poll-seconds}")));
    }

    @Test
    @DisplayName("設定された受信件数とロングポーリング秒数をSQSへ渡す")
    void usesConfiguredReceiveOptions() {
        // given
        var sqsClient = mock(SqsClient.class);
        stubQueueUrls(sqsClient);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().build());
        var scheduler =
                new SqsSimulationScheduler(
                        sqsClient,
                        new ObjectMapper(),
                        mock(SimulateGameUseCase.class),
                        mock(LineUpMapper.class),
                        "request-queue",
                        "result-queue",
                        3,
                        7);

        // when
        scheduler.poll();

        // then
        var request = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqsClient).receiveMessage(request.capture());
        assertAll(
                () -> assertEquals(3, request.getValue().maxNumberOfMessages()),
                () -> assertEquals(7, request.getValue().waitTimeSeconds()));
    }

    @Test
    @DisplayName("受信した試合を実行すると結果を指定されたSQSへ送信して元メッセージを削除する")
    void sendsSimulationResponseAndDeletesReceivedMessage() throws Exception {
        // given
        SqsClient sqsClient = mock(SqsClient.class);
        SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
        HittingStrategy hittingStrategy = BehaviorStrategies.middleDistanceHittingStrategy();
        StealStrategy stealStrategy = BehaviorStrategies.eagerSteal();
        LineUpMapper mapper =
                new LineUpMapper(hittingStrategy, stealStrategy, BehaviorStrategies.standardBunt());
        ObjectMapper objectMapper = new ObjectMapper();
        List<SimulationPlayerMessage> players =
                IntStream.rangeClosed(1, 9)
                        .mapToObj(
                                number ->
                                        new SimulationPlayerMessage(
                                                "player-" + number,
                                                0.3f,
                                                0.4f,
                                                0.7f,
                                                true,
                                                0.8f,
                                                true))
                        .toList();
        UUID simulationId = UUID.randomUUID();
        String body =
                objectMapper.writeValueAsString(
                        new SimulationRequestMessage(simulationId, "1", players));
        Message message = Message.builder().body(body).receiptHandle("receipt-1").build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        List<SimulationResponse> simulationResponses =
                IntStream.range(0, 10)
                        .mapToObj(
                                index ->
                                        new SimulationResponse(
                                                index,
                                                4,
                                                new GameStatistics(
                                                        1, 1, 0, 0, 0, 2, 3, 5, 7, 11, 13, 17, 19,
                                                        23, 29)))
                        .toList();
        when(useCase.invoke(any(LineUpEntity.class)))
                .thenReturn(simulationResult(simulationResponses));
        SqsSimulationScheduler sut =
                new SqsSimulationScheduler(
                        sqsClient,
                        objectMapper,
                        useCase,
                        mapper,
                        "request-queue",
                        "result-queue",
                        10,
                        10);
        stubQueueUrls(sqsClient);
        var lineUpCaptor = ArgumentCaptor.forClass(LineUpEntity.class);
        var sendMessageCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        var ordered = inOrder(useCase, sqsClient);

        // when
        sut.poll();

        // then
        ordered.verify(useCase).invoke(lineUpCaptor.capture());
        ordered.verify(sqsClient, org.mockito.Mockito.times(1))
                .sendMessage(sendMessageCaptor.capture());
        ordered.verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
        List<SimulationResultMessage> sentResponses =
                sendMessageCaptor.getAllValues().stream()
                        .map(
                                request -> {
                                    try {
                                        return objectMapper.readValue(
                                                request.messageBody(),
                                                SimulationResultMessage.class);
                                    } catch (JsonProcessingException exception) {
                                        throw new IllegalArgumentException(exception);
                                    }
                                })
                        .toList();
        var sentJson = objectMapper.readTree(sendMessageCaptor.getValue().messageBody());
        assertAll(
                () -> assertEquals(9, lineUpCaptor.getValue().getBatterEntities().size()),
                () -> assertEquals("result-url", sendMessageCaptor.getValue().queueUrl()),
                () -> assertEquals(1, sentResponses.size()),
                () -> assertEquals("2", sentResponses.getFirst().version()),
                () -> assertEquals(simulationId, sentResponses.getFirst().simulationId()),
                () -> assertEquals(10, sentResponses.getFirst().gameScoreStatistics().gameCount()),
                () ->
                        assertEquals(
                                10,
                                sentResponses
                                        .getFirst()
                                        .gameScoreStatistics()
                                        .scoreDistribution()
                                        .values()
                                        .stream()
                                        .mapToInt(Integer::intValue)
                                        .sum()),
                () ->
                        assertEquals(
                                10,
                                sentResponses.getFirst().gameContentStatistics().homeRunCount()),
                () ->
                        assertEquals(
                                20, sentResponses.getFirst().gameContentStatistics().buntCount()),
                () ->
                        assertEquals(
                                30, sentResponses.getFirst().gameContentStatistics().stealCount()),
                () ->
                        assertEquals(
                                50,
                                sentResponses
                                        .getFirst()
                                        .gameContentStatistics()
                                        .buntFailureCount()),
                () ->
                        assertEquals(
                                70,
                                sentResponses
                                        .getFirst()
                                        .gameContentStatistics()
                                        .stealFailureCount()),
                () ->
                        assertEquals(
                                110,
                                sentResponses
                                        .getFirst()
                                        .gameContentStatistics()
                                        .advancingBuntCount()),
                () ->
                        assertEquals(
                                130,
                                sentResponses
                                        .getFirst()
                                        .gameContentStatistics()
                                        .squeezeBuntCount()),
                () ->
                        assertEquals(
                                170,
                                sentResponses
                                        .getFirst()
                                        .gameContentStatistics()
                                        .advancingBuntFailureCount()),
                () ->
                        assertEquals(
                                190,
                                sentResponses
                                        .getFirst()
                                        .gameContentStatistics()
                                        .squeezeBuntFailureCount()),
                () ->
                        assertEquals(
                                230,
                                sentResponses
                                        .getFirst()
                                        .gameContentStatistics()
                                        .stealToSecondCount()),
                () ->
                        assertEquals(
                                290,
                                sentResponses
                                        .getFirst()
                                        .gameContentStatistics()
                                        .stealToThirdCount()),
                () -> assertEquals(10, sentJson.at("/gameScoreStatistics/gameCount").intValue()),
                () ->
                        assertEquals(
                                110,
                                sentJson.at("/gameContentStatistics/advancingBuntCount")
                                        .intValue()),
                () ->
                        assertEquals(
                                130,
                                sentJson.at("/gameContentStatistics/squeezeBuntCount").intValue()),
                () ->
                        assertEquals(
                                170,
                                sentJson.at("/gameContentStatistics/advancingBuntFailureCount")
                                        .intValue()),
                () ->
                        assertEquals(
                                190,
                                sentJson.at("/gameContentStatistics/squeezeBuntFailureCount")
                                        .intValue()),
                () ->
                        assertEquals(
                                230,
                                sentJson.at("/gameContentStatistics/stealToSecondCount")
                                        .intValue()),
                () ->
                        assertEquals(
                                290,
                                sentJson.at("/gameContentStatistics/stealToThirdCount").intValue()),
                () -> assertEquals(true, sentJson.path("statistics").isMissingNode()));
    }

    @Test
    @DisplayName("先行メッセージでJVM致命Errorが発生しても後続メッセージを処理する")
    void continuesWithNextMessageWhenPreviousMessageRaisesError() throws Exception {
        // given
        SqsClient sqsClient = mock(SqsClient.class);
        SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
        LineUpMapper mapper = mock(LineUpMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        var request = new SimulationRequestMessage(UUID.randomUUID(), "1", List.of());
        String body = objectMapper.writeValueAsString(request);
        Message failingMessage =
                Message.builder()
                        .messageId("failing")
                        .body(body)
                        .receiptHandle("receipt-1")
                        .build();
        Message succeedingMessage =
                Message.builder()
                        .messageId("succeeding")
                        .body(body)
                        .receiptHandle("receipt-2")
                        .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(
                        ReceiveMessageResponse.builder()
                                .messages(failingMessage, succeedingMessage)
                                .build());
        when(useCase.invoke(any()))
                .thenThrow(new OutOfMemoryError("fatal simulation failure"))
                .thenReturn(simulationResult(List.of(new SimulationResponse(5, 4))));
        stubQueueUrls(sqsClient);
        var scheduler =
                new SqsSimulationScheduler(
                        sqsClient,
                        objectMapper,
                        useCase,
                        mapper,
                        "request-queue",
                        "result-queue",
                        10,
                        10);
        var deleteCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);

        // when
        scheduler.poll();

        // then
        assertAll(
                () -> verify(useCase, org.mockito.Mockito.times(2)).invoke(any()),
                () -> verify(sqsClient).sendMessage(any(SendMessageRequest.class)),
                () -> verify(sqsClient).deleteMessage(deleteCaptor.capture()),
                () -> assertEquals("receipt-2", deleteCaptor.getValue().receiptHandle()));
    }

    @Test
    @DisplayName("シミュレーション結果をJSONへ変換できない場合はSQSへ送信せず元メッセージも削除しない")
    void doesNotSendOrDeleteWhenResponseSerializationFails() throws Exception {
        // given
        SqsClient sqsClient = mock(SqsClient.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
        LineUpMapper mapper = mock(LineUpMapper.class);
        var request = new SimulationRequestMessage(UUID.randomUUID(), "1", List.of());
        var responses = List.of(new SimulationResponse(5, 4));
        Message message = Message.builder().body("request-body").receiptHandle("receipt-1").build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(objectMapper.readValue("request-body", SimulationRequestMessage.class))
                .thenReturn(request);
        when(useCase.invoke(any())).thenReturn(simulationResult(responses));
        when(objectMapper.writeValueAsString(any(SimulationResultMessage.class)))
                .thenThrow(new JsonProcessingException("serialization failed") {});
        SqsSimulationScheduler scheduler =
                new SqsSimulationScheduler(
                        sqsClient,
                        objectMapper,
                        useCase,
                        mapper,
                        "request-queue",
                        "result-queue",
                        10,
                        10);
        stubQueueUrls(sqsClient);

        // when
        scheduler.poll();

        // then
        assertAll(
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
        var request = new SimulationRequestMessage(UUID.randomUUID(), "1", List.of());
        var responses = List.of(new SimulationResponse(5, 4));
        Message message =
                Message.builder()
                        .body(objectMapper.writeValueAsString(request))
                        .receiptHandle("receipt-1")
                        .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(useCase.invoke(any())).thenReturn(simulationResult(responses));
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(SqsException.builder().message("send failed").build());
        SqsSimulationScheduler scheduler =
                new SqsSimulationScheduler(
                        sqsClient,
                        objectMapper,
                        useCase,
                        mapper,
                        "request-queue",
                        "result-queue",
                        10,
                        10);
        stubQueueUrls(sqsClient);

        // when
        scheduler.poll();

        // then
        assertAll(
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
                new SqsSimulationScheduler(
                        sqsClient,
                        objectMapper,
                        useCase,
                        mapper,
                        "request-queue",
                        "result-queue",
                        10,
                        10);
        stubQueueUrls(sqsClient);

        // when
        scheduler.poll();

        // then
        assertAll(
                () -> verifyNoInteractions(useCase),
                () -> verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class)),
                () -> verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class)));
    }

    @Test
    @DisplayName("統計値を計算できない場合は結果を送信せず要求を削除しない")
    void doesNotSendOrDeleteWhenStatisticsCannotBeCalculated() throws Exception {
        // given
        SqsClient sqsClient = mock(SqsClient.class);
        SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
        LineUpMapper mapper = mock(LineUpMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        var request = new SimulationRequestMessage(UUID.randomUUID(), "1", List.of());
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(
                        ReceiveMessageResponse.builder()
                                .messages(
                                        Message.builder()
                                                .body(objectMapper.writeValueAsString(request))
                                                .receiptHandle("receipt-1")
                                                .build())
                                .build());
        when(useCase.invoke(any()))
                .thenThrow(new IllegalArgumentException("scores must not be empty"));
        stubQueueUrls(sqsClient);
        var scheduler =
                new SqsSimulationScheduler(
                        sqsClient,
                        objectMapper,
                        useCase,
                        mapper,
                        "request-queue",
                        "result-queue",
                        10,
                        10);
        var ordered = inOrder(sqsClient);

        // when
        scheduler.poll();

        // then
        assertAll(
                () -> ordered.verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class)),
                () ->
                        ordered.verify(sqsClient, never())
                                .deleteMessage(any(DeleteMessageRequest.class)));
    }
}
