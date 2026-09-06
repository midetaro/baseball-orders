package com.example.baseballorders.backend.simulation.domain;

import java.util.List;
import java.util.UUID;

/**
 * backendがHTTP要求へ返すシミュレーション結果。
 *
 * @param simulationId シミュレーションの相関ID
 * @param results 実行順の得点・失点の組
 */
public record SimulationResult(UUID simulationId, List<Result> results) {
    /**
     * 1試合の結果。
     *
     * @param score 得点
     * @param runs 失点
     */
    public record Result(int score, int runs) {}
}
