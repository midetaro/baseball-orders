package com.example.baseballorders.simulator.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code simulation.pitcher} 配下の、対戦する投手の性格ごとの打撃・走塁補正倍率。
 *
 * @param bold 力勝負型の投手に対する補正
 * @param cautious 慎重型の投手に対する補正
 * @param technical 技巧型の投手に対する補正
 * @param standard 投手を指定しない既定状態の補正
 */
@ConfigurationProperties(prefix = "simulation.pitcher")
public record SimulationPitcherProperties(
        Multipliers bold, Multipliers cautious, Multipliers technical, Multipliers standard) {

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
