package com.example.baseballorders.backend.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 認証済みユーザーのシミュレーション集計値。 */
public record SimulationStatistics(
        UUID simulationId,
        Long userId,
        double averageScore,
        double medianScore,
        int maximumScore,
        Instant createdAt) {
    /** 必須項目を検証して統計情報を作成する。 */
    public SimulationStatistics {
        Objects.requireNonNull(simulationId, "simulationId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /** シミュレーション結果の集計値から統計情報を作成する。 */
    public static SimulationStatistics create(
            Long userId, SimulationResult result, Instant createdAt) {
        return new SimulationStatistics(
                result.simulationId(),
                userId,
                result.statistics().averageScore(),
                result.statistics().medianScore(),
                result.statistics().maximumScore(),
                createdAt);
    }
}
