package com.example.baseballorders.backend.application.exception;

import java.util.UUID;

/** 許容時間内にsimulatorからシミュレーション結果を受け取れなかったことを表す。 */
public final class SimulationTimeoutException extends RuntimeException {

    /**
     * timeoutしたsimulation IDを含む例外を作成する。
     *
     * @param simulationId timeoutした相関ID
     */
    public SimulationTimeoutException(UUID simulationId) {
        super("simulation result was not received within the allowed time: " + simulationId);
    }
}
