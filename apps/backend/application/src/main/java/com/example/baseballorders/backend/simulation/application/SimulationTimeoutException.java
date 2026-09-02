package com.example.baseballorders.backend.simulation.application;

import java.util.UUID;

/** 許容時間内にsimulation-resultを取得できなかったことを表す。 */
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
