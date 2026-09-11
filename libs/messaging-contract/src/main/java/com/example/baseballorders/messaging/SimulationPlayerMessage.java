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
 */
public record SimulationPlayerMessage(
        String name,
        float hitAverage,
        float sluggish,
        float buntSuccessRate,
        Boolean buntEnabled,
        float stealSuccessRate) {

    /**
     * Ensures every simulation player explicitly declares whether to attempt bunts.
     *
     * @throws NullPointerException when {@code buntEnabled} is absent
     */
    public SimulationPlayerMessage {
        Objects.requireNonNull(buntEnabled, "buntEnabled must not be null");
    }
}
