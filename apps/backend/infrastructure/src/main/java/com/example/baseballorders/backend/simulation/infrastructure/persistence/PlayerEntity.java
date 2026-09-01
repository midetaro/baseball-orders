package com.example.baseballorders.backend.simulation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** playersテーブルへJPAでマッピングする選手Entity。 */
@Getter
@Entity
@Table(name = "players")
public class PlayerEntity {

    @Id
    @Column(name = "player_id", nullable = false)
    private String playerId;

    @Column(nullable = false)
    private String name;

    @Column(name = "hit_average", nullable = false)
    private float hitAverage;

    @Column(nullable = false)
    private float sluggish;

    /** JPAがEntityを復元するために使用する。 */
    protected PlayerEntity() {}

    PlayerEntity(String playerId, String name, float hitAverage, float sluggish) {
        this.playerId = playerId;
        this.name = name;
        this.hitAverage = hitAverage;
        this.sluggish = sluggish;
    }
}
