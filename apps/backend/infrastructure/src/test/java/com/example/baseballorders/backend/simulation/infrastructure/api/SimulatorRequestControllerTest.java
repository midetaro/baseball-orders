package com.example.baseballorders.backend.simulation.infrastructure.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.baseballorders.backend.simulation.infrastructure.messaging.PlayerData;
import com.example.baseballorders.backend.simulation.infrastructure.messaging.SimulationRequest;
import com.example.baseballorders.backend.simulation.infrastructure.messaging.SimulatorRequestSender;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

class SimulatorRequestControllerTest {

    @Test
    @DisplayName("シミュレーションAPIに9人の選手データを送るとsimulatorへ送信される")
    void sendsApiRequestToSimulator() {
        // given
        var publishedMessages = new ArrayList<SimulationRequest>();
        var controller =
                new SimulatorRequestController(new SimulatorRequestSender(publishedMessages::add));
        var players = players();

        // when
        String simulationId = controller.send(players);

        // then
        assertAll(
                () -> assertEquals(simulationId, publishedMessages.getFirst().simulationId()),
                () -> assertEquals(players, publishedMessages.getFirst().players()));
    }

    @Test
    @DisplayName("ControllerはPOSTのシミュレーションAPIとして公開される")
    void exposesPostSimulationApi() throws NoSuchMethodException {
        // given
        var controllerType = SimulatorRequestController.class;
        var sendMethod = controllerType.getMethod("send", List.class);

        // when
        var restController = controllerType.getAnnotation(RestController.class);
        var requestMapping = controllerType.getAnnotation(RequestMapping.class);
        var postMapping = sendMethod.getAnnotation(PostMapping.class);
        var requestBody = sendMethod.getParameters()[0].getAnnotation(RequestBody.class);
        var responseStatus = sendMethod.getAnnotation(ResponseStatus.class);

        // then
        assertAll(
                () -> assertEquals(RestController.class, restController.annotationType()),
                () -> assertEquals(List.of("/simulations"), List.of(requestMapping.value())),
                () -> assertEquals(PostMapping.class, postMapping.annotationType()),
                () -> assertEquals(RequestBody.class, requestBody.annotationType()),
                () -> assertEquals(HttpStatus.ACCEPTED, responseStatus.value()));
    }

    @Test
    @DisplayName("送信機能がnullの場合はControllerを作成できない")
    void rejectsNullSender() {
        // given

        // when
        var exception =
                assertThrows(
                        NullPointerException.class, () -> new SimulatorRequestController(null));

        // then
        assertAll(() -> assertEquals("sender must not be null", exception.getMessage()));
    }

    private static List<PlayerData> players() {
        return java.util.stream.IntStream.rangeClosed(1, 9)
                .mapToObj(number -> new PlayerData("player-" + number, 0.3f, 0.4f))
                .toList();
    }
}
