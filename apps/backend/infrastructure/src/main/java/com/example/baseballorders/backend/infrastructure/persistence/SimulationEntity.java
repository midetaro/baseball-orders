package com.example.baseballorders.backend.infrastructure.persistence;

import com.example.baseballorders.backend.domain.SimulationStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** simulationsテーブルへJPAでマッピングするEntity。 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "simulations")
public class SimulationEntity {
    @Id
    @Column(name = "simulation_id", length = 100)
    private UUID simulationId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SimulationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** 保存対象のシミュレーションEntityを作成する。 */
    public SimulationEntity(
            UUID simulationId,
            Long userId,
            SimulationStatus status,
            Instant createdAt,
            Instant completedAt) {
        this.simulationId = simulationId;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }
}
