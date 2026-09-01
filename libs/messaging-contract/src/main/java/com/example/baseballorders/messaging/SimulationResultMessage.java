package com.example.baseballorders.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * simulatorからbackendへ返す共有結果メッセージ。
 *
 * @param simulationId 要求と同じ相関ID
 * @param version メッセージスキーマのバージョン
 * @param score 得点
 * @param runs 失点
 */
public record SimulationResultMessage(
        @JsonProperty("simulation_id") UUID simulationId, String version, int score, int runs) {}
