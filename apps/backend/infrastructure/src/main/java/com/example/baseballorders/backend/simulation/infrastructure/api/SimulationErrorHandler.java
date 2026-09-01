package com.example.baseballorders.backend.simulation.infrastructure.api;

import com.example.baseballorders.backend.simulation.infrastructure.messaging.SimulationSendException;
import com.example.baseballorders.backend.simulation.infrastructure.messaging.SimulationTimeoutException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 同期シミュレーションAPIのtimeoutとSQS送信失敗をHTTPエラーへ変換する。 */
@RestControllerAdvice
public final class SimulationErrorHandler {

    /**
     * 結果timeoutを504 Gateway Timeoutとして返す。
     *
     * @param exception timeout例外
     * @return エラー内容を持つHTTP 504応答
     */
    @ExceptionHandler(SimulationTimeoutException.class)
    public ResponseEntity<Map<String, String>> handleTimeout(SimulationTimeoutException exception) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(Map.of("error", exception.getMessage()));
    }

    /**
     * SQS送信失敗を502 Bad Gatewayとして返す。
     *
     * @param exception 送信失敗例外
     * @return エラー内容を持つHTTP 502応答
     */
    @ExceptionHandler(SimulationSendException.class)
    public ResponseEntity<Map<String, String>> handleSendFailure(
            SimulationSendException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", exception.getMessage()));
    }
}
