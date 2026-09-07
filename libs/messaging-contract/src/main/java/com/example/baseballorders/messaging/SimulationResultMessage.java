package com.example.baseballorders.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * simulatorからbackendへ返す共有結果メッセージ。
 *
 * @param simulationId 要求と同じ相関ID
 * @param version メッセージスキーマのバージョン
 * @param results 実行順の得点・失点の組
 * @param statistics 全試合の得点統計
 */
public record SimulationResultMessage(
        @JsonProperty("simulation_id") UUID simulationId,
        String version,
        List<Result> results,
        Statistics statistics) {

    /**
     * 全試合の得点統計。
     *
     * @param averageScore 平均得点
     * @param medianScore 中央値得点
     * @param maximumScore 最大得点
     */
    public record Statistics(double averageScore, double medianScore, int maximumScore) {}

    /**
     * Backwards-compatible constructor for callers that do not yet supply statistics.
     *
     * @param simulationId 要求と同じ相関ID
     * @param version メッセージスキーマのバージョン
     * @param results 実行順の得点・失点の組
     */
    public SimulationResultMessage(UUID simulationId, String version, List<Result> results) {
        this(simulationId, version, results, null);
    }

    /**
     * 1試合の結果。
     *
     * @param score 得点
     * @param runs 失点
     */
    public record Result(int score, int runs) {}
}
