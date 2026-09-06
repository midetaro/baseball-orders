package com.example.baseballorders.backend.application.exception;

import java.util.UUID;

public class SimulationAcceptException extends RuntimeException {
    /**
     * 送信失敗の相関IDと原因を保持する例外を作成する。
     *
     * @param simulationId 送信できなかった相関ID
     * @param cause SQS送信時の原因
     */
    public SimulationAcceptException(UUID simulationId, RuntimeException cause) {
        super("simulation request could not be sent: " + simulationId, cause);
    }
}
