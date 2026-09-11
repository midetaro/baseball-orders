package com.example.baseballorders.backend.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** simulation_statisticsテーブルへJPAでマッピングするEntity。 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "simulation_statistics",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_simulation_statistics_simulation",
                        columnNames = "simulation_id"))
public class SimulationStatisticsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "simulation_id", nullable = false, length = 100)
    private UUID simulationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "average_score", nullable = false)
    private double averageScore;

    @Column(name = "median_score", nullable = false)
    private double medianScore;

    @Column(name = "maximum_score", nullable = false)
    private int maximumScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 保存対象の統計情報Entityを作成する。 */
    public SimulationStatisticsEntity(
            UUID simulationId,
            Long userId,
            double averageScore,
            double medianScore,
            int maximumScore,
            Instant createdAt) {
        this.simulationId = simulationId;
        this.userId = userId;
        this.averageScore = averageScore;
        this.medianScore = medianScore;
        this.maximumScore = maximumScore;
        this.createdAt = createdAt;
    }
}
