package com.example.baseballorders.backend.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** シミュレーション要求の所有者と完了状態を表す。 */
public record Simulation(
        UUID simulationId,
        Long userId,
        SimulationStatus status,
        Instant createdAt,
        Instant completedAt) {
    /** 必須項目を検証してシミュレーションを作成する。 */
    public Simulation {
        Objects.requireNonNull(simulationId, "simulationId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /** 完了状態に遷移したシミュレーションを返す。 */
    public Simulation complete(Instant completedAt) {
        return new Simulation(
                simulationId, userId, SimulationStatus.COMPLETED, createdAt, completedAt);
    }
}
