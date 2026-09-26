package com.example.baseballorders.simulator.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code simulation.pitcher} 配下の、対戦する投手の打撃・走塁補正倍率。
 *
 * <p>simulationは対戦相手の投手の性格という入力を無視するため、既定状態の補正のみを保持する。
 *
 * @param standard 対戦する投手を区別しない既定の補正
 */
@ConfigurationProperties(prefix = "simulation.pitcher")
public record SimulationPitcherProperties(Multipliers standard) {

    /**
     * 1つの投手性格に対する補正倍率。
     *
     * @param onBaseMultiplier 出塁率へ掛ける倍率
     * @param sluggingMultiplier 長打率へ掛ける倍率
     * @param runningMultiplier バント・盗塁成功率へ掛ける倍率
     */
    public record Multipliers(
            float onBaseMultiplier, float sluggingMultiplier, float runningMultiplier) {}
}
