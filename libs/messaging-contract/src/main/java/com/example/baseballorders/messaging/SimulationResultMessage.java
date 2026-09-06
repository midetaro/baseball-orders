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
 */
public record SimulationResultMessage(
        @JsonProperty("simulation_id") UUID simulationId, String version, List<Result> results) {

    /**
     * 1試合の結果。
     *
     * @param score 得点
     * @param runs 失点
     */
    public record Result(int score, int runs) {}
}
