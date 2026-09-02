package com.example.baseballorders.backend.simulation.infrastructure.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.backend.simulation.application.SimulationSendException;
import com.example.baseballorders.backend.simulation.application.SimulationTimeoutException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SimulationErrorHandlerTest {

    @Test
    @DisplayName("結果timeoutは説明を含むHTTP 504応答へ変換される")
    void convertsTimeoutToGatewayTimeout() {
        // given
        var handler = new SimulationErrorHandler();
        var exception = new SimulationTimeoutException(UUID.randomUUID());

        // when
        var response = handler.handleTimeout(exception);

        // then
        assertAll(
                () -> assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode()),
                () -> assertEquals(exception.getMessage(), response.getBody().get("error")));
    }

    @Test
    @DisplayName("SQS送信失敗は説明を含むHTTP 502応答へ変換される")
    void convertsSendFailureToBadGateway() {
        // given
        var handler = new SimulationErrorHandler();
        var exception =
                new SimulationSendException(
                        UUID.randomUUID(), new IllegalStateException("send failed"));

        // when
        var response = handler.handleSendFailure(exception);

        // then
        assertAll(
                () -> assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode()),
                () -> assertEquals(exception.getMessage(), response.getBody().get("error")));
    }
}
