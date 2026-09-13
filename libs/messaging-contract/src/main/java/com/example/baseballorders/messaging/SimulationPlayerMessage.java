package com.example.baseballorders.messaging;

import java.util.Objects;

/**
 * シミュレーション要求で共有する選手データ。
 *
 * @param name 選手名
 * @param hitAverage 打率
 * @param sluggish 長打率
 * @param buntSuccessRate バント成功率
 * @param buntEnabled バントを試みるかどうか
 * @param stealSuccessRate 盗塁成功率
 * @param stealEnabled 盗塁を試みるかどうか
 */
public record SimulationPlayerMessage(
        String name,
        Float hitAverage,
        Float sluggish,
        Float buntSuccessRate,
        Boolean buntEnabled,
        Float stealSuccessRate,
        Boolean stealEnabled) {

    /**
     * Rejects missing player data, including rates when a strategy is disabled.
     *
     * @throws NullPointerException when any player field is absent
     */
    public SimulationPlayerMessage {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(hitAverage, "hitAverage must not be null");
        Objects.requireNonNull(sluggish, "sluggish must not be null");
        Objects.requireNonNull(buntSuccessRate, "buntSuccessRate must not be null");
        Objects.requireNonNull(buntEnabled, "buntEnabled must not be null");
        Objects.requireNonNull(stealSuccessRate, "stealSuccessRate must not be null");
        Objects.requireNonNull(stealEnabled, "stealEnabled must not be null");
    }
}
