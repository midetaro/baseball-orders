package com.example.baseballorders.backend.infrastructure.api;

import static org.junit.jupiter.api.Assertions.*;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.domain.PitcherPersonality;
import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.example.baseballorders.backend.domain.SimulationResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

class SimulatorRequestControllerTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "true,true",
        "true,false",
        "false,true",
        "false,false"
    })
    @DisplayName("画面の盗塁・バント選択と性格を共有メッセージまで保持する")
    void propagatesStrategyOptions(boolean stealEnabled, boolean buntEnabled) {
        // given
        var template = org.mockito.Mockito.mock(io.awspring.cloud.sqs.operations.SqsTemplate.class);
        var publisher =
                new com.example.baseballorders.backend.infrastructure.messaging
                        .SqsSimulatorMessagePublisher(template, "request");
        var registry = new WaitingResultRegistry();
        var controller =
                new SimulatorRequestController(
                        new SimulationCoordinator(
                                request -> {
                                    publisher.publish(request);
                                    registry.complete(
                                            request.simulationId(),
                                            new SimulationResult(
                                                    request.simulationId(),
                                                    List.of(new SimulationResult.Result(0, 0)),
                                                    new SimulationResult.Statistics(0, 0, 0)));
                                },
                                registry));
        var captor =
                org.mockito.ArgumentCaptor.forClass(
                        com.example.baseballorders.messaging.SimulationRequestMessage.class);

        // when
        controller.send(
                playersWith(
                        new PlayerInputRequest(
                                0.3f,
                                0.4f,
                                0.7f,
                                0.7f,
                                buntEnabled,
                                stealEnabled,
                                PlayerPersonality.EAGER_STEAL)));

        // then
        org.mockito.Mockito.verify(template)
                .send(org.mockito.ArgumentMatchers.eq("request"), captor.capture());
        var json = new tools.jackson.databind.ObjectMapper().valueToTree(captor.getValue());
        assertAll(
                () ->
                        assertEquals(
                                stealEnabled,
                                json.path("players").get(0).required("stealEnabled").asBoolean()),
                () ->
                        assertEquals(
                                buntEnabled,
                                json.path("players").get(0).required("buntEnabled").asBoolean()),
                () ->
                        assertEquals(
                                "EAGER_STEAL",
                                json.path("players").get(0).required("personality").asText()),
                () -> assertEquals("1番", captor.getValue().players().getFirst().name()),
                () -> assertEquals(9, captor.getValue().players().size()));
    }

    @Test
    @DisplayName("画面入力した打順データをSQS結果を待機して同期的に返す")
    void returnsSynchronousSimulationResult() {
        // given
        var registry = new WaitingResultRegistry();
        var coordinator =
                new SimulationCoordinator(
                        request ->
                                registry.complete(
                                        request.simulationId(),
                                        new SimulationResult(
                                                request.simulationId(),
                                                List.of(new SimulationResult.Result(5, 4)),
                                                new SimulationResult.Statistics(5, 5, 5))),
                        registry);
        var controller = new SimulatorRequestController(coordinator);

        // when
        SimulationResult result =
                controller.send(
                        java.util.stream.IntStream.rangeClosed(1, 9)
                                .mapToObj(
                                        number ->
                                                new PlayerInputRequest(
                                                        0.300f, 0.400f, 0.700f, 0.700f, true, true))
                                .toList());

        // then
        assertAll(
                () -> assertEquals(5, result.statistics().averageScore()),
                () -> assertEquals(5, result.statistics().maximumScore()));
    }

    @Test
    @DisplayName("範囲外または未入力の打撃データを拒否する")
    void rejectsInvalidPlayerInput() {
        // given
        var registry = new WaitingResultRegistry();
        var controller =
                new SimulatorRequestController(new SimulationCoordinator(request -> {}, registry));

        // when
        var hitAverageException =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                controller.send(
                                        playersWith(
                                                new PlayerInputRequest(
                                                        -0.001f, 0.400f, 0.700f, 0.700f, false,
                                                        false))));
        var missingValueException =
                assertThrows(
                        NullPointerException.class,
                        () ->
                                controller.send(
                                        playersWith(
                                                new PlayerInputRequest(
                                                        0.300f, 0.400f, null, 0.700f, false,
                                                        false))));
        var buntSuccessRateException =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                controller.send(
                                        playersWith(
                                                new PlayerInputRequest(
                                                        0.300f, 0.400f, 0.701f, 0.700f, false,
                                                        false))));
        var stealSuccessRateException =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                controller.send(
                                        playersWith(
                                                new PlayerInputRequest(
                                                        0.300f, 0.400f, 0.700f, 0.701f, false,
                                                        false))));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "hitAverage must be between 0.0 and 1.0",
                                hitAverageException.getMessage()),
                () ->
                        assertEquals(
                                "bunt_success_rate must not be null",
                                missingValueException.getMessage()),
                () ->
                        assertEquals(
                                "buntSuccessRate must be between 0.0 and 0.7",
                                buntSuccessRateException.getMessage()),
                () ->
                        assertEquals(
                                "stealSuccessRate must be between 0.0 and 0.7",
                                stealSuccessRateException.getMessage()));
    }

    @Test
    @DisplayName("POST APIは202ではなく結果を返す通常の同期エンドポイントである")
    void exposesSynchronousPostEndpoint() throws NoSuchMethodException {
        // given
        var method =
                SimulatorRequestController.class.getMethod(
                        "send", List.class, PitcherPersonality.class);

        // when
        var postMapping = method.getAnnotation(PostMapping.class);

        // then
        assertAll(
                () -> assertEquals(PostMapping.class, postMapping.annotationType()),
                () -> assertEquals(SimulationResult.class, method.getReturnType()));
    }

    @Test
    @DisplayName("HTTP入力の投手性格を共有メッセージまで保持する")
    void propagatesPitcherPersonality() {
        // given
        var template = org.mockito.Mockito.mock(io.awspring.cloud.sqs.operations.SqsTemplate.class);
        var publisher =
                new com.example.baseballorders.backend.infrastructure.messaging
                        .SqsSimulatorMessagePublisher(template, "request");
        var registry = new WaitingResultRegistry();
        var controller =
                new SimulatorRequestController(
                        new SimulationCoordinator(
                                request -> {
                                    publisher.publish(request);
                                    registry.complete(
                                            request.simulationId(),
                                            new SimulationResult(
                                                    request.simulationId(),
                                                    List.of(new SimulationResult.Result(0, 0)),
                                                    new SimulationResult.Statistics(0, 0, 0)));
                                },
                                registry));
        var captor =
                org.mockito.ArgumentCaptor.forClass(
                        com.example.baseballorders.messaging.SimulationRequestMessage.class);

        // when
        controller.send(
                playersWith(new PlayerInputRequest(0.3f, 0.4f, 0.7f, 0.7f, true, true)),
                PitcherPersonality.CAUTIOUS);

        // then
        org.mockito.Mockito.verify(template)
                .send(org.mockito.ArgumentMatchers.eq("request"), captor.capture());
        assertAll(
                () ->
                        assertEquals(
                                com.example.baseballorders.messaging.PitcherPersonality.CAUTIOUS,
                                captor.getValue().pitcherPersonality()));
    }

    private List<PlayerInputRequest> playersWith(PlayerInputRequest player) {
        return java.util.stream.IntStream.range(0, 9).mapToObj(ignored -> player).toList();
    }
}
