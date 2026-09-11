package com.example.baseballorders.backend.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * backendがHTTP要求へ返すシミュレーション結果。
 *
 * @param simulationId シミュレーションの相関ID
 * @param results 実行順の得点・失点の組
 * @param statistics 全試合の得点統計
 */
public record SimulationResult(UUID simulationId, List<Result> results, Statistics statistics) {
    /** 統計情報を必須にしてシミュレーション結果を作成する。 */
    public SimulationResult {
        Objects.requireNonNull(simulationId, "simulationId must not be null");
        Objects.requireNonNull(results, "results must not be null");
        Objects.requireNonNull(statistics, "statistics must not be null");
    }

    /**
     * 全試合の得点統計。
     *
     * @param averageScore 平均得点
     * @param medianScore 中央値得点
     * @param maximumScore 最大得点
     */
    public record Statistics(double averageScore, double medianScore, int maximumScore) {}

    /**
     * 1試合の結果。
     *
     * @param score 得点
     * @param runs 失点
     */
    public record Result(int score, int runs) {}
}
