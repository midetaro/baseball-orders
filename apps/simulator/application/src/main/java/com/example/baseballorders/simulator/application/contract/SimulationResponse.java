package com.example.baseballorders.simulator.application.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of a completed game simulation.
 *
 * @param simulationId シミュレーションを識別するID
 * @param score 得点
 * @param runs 失点
 */
public record SimulationResponse(
        @JsonProperty("simulation_id") String simulationId, int score, int runs) {

    /**
     * シミュレーションIDを付与する前の試合結果を作成する。
     *
     * @param score 得点
     * @param runs 失点
     */
    public SimulationResponse(int score, int runs) {
        this(null, score, runs);
    }
}
