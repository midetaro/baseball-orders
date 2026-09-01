package com.example.baseballorders.backend.simulation.infrastructure.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.backend.simulation.domain.SimulationResult;
import com.example.baseballorders.backend.simulation.infrastructure.messaging.SimulationCoordinator;
import com.example.baseballorders.backend.simulation.infrastructure.messaging.WaitingResultRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

class SimulatorRequestControllerTest {

    @Test
    @DisplayName("player_idを受け取るとSQS結果を待機して同期的に返す")
    void returnsSynchronousSimulationResult() {
        // given
        var registry = new WaitingResultRegistry();
        var coordinator =
                new SimulationCoordinator(
                        ids -> List.of(),
                        request ->
                                registry.complete(
                                        request.simulationId(),
                                        new SimulationResult(request.simulationId(), 5, 4)),
                        registry);
        var controller = new SimulatorRequestController(coordinator);

        // when
        SimulationResult result =
                controller.send(
                        java.util.stream.IntStream.rangeClosed(1, 9)
                                .mapToObj(number -> new PlayerIdRequest("player-" + number))
                                .toList());

        // then
        assertAll(() -> assertEquals(5, result.score()), () -> assertEquals(4, result.runs()));
    }

    @Test
    @DisplayName("POST APIは202ではなく結果を返す通常の同期エンドポイントである")
    void exposesSynchronousPostEndpoint() throws NoSuchMethodException {
        // given
        var method = SimulatorRequestController.class.getMethod("send", List.class);

        // when
        var postMapping = method.getAnnotation(PostMapping.class);

        // then
        assertAll(
                () -> assertEquals(PostMapping.class, postMapping.annotationType()),
                () -> assertEquals(SimulationResult.class, method.getReturnType()));
    }
}
