package com.example.baseballorders.backend.simulation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** playersテーブルへJPAでマッピングする選手Entity。 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Entity
@Table(name = "players")
public class PlayerEntity {

    @Id
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private String name;

    @Column(name = "hit_average", nullable = false)
    private float hitAverage;

    @Column(nullable = false)
    private float sluggish;

    @Column(name = "bunt_success_rate", nullable = false)
    private float buntSuccessRate;

    @Column(name = "steal_success_rate", nullable = false)
    private float stealSuccessRate;
}
