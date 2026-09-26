package com.example.baseballorders.simulator.infrastructure.config;

import com.example.baseballorders.simulator.infrastructure.config.SimulationPitcherProperties.Multipliers;

/** 本番の {@code application.yml} と同じ投手補正倍率をテストへ供給するフィクスチャ。 */
public final class SimulationPropertiesTestData {

    private SimulationPropertiesTestData() {}

    /**
     * 本番既定値と同じ投手補正倍率を返す。
     *
     * @return 標準の補正倍率設定
     */
    public static SimulationPitcherProperties standardPitcherProperties() {
        return new SimulationPitcherProperties(new Multipliers(1.0f, 1.0f, 1.0f));
    }
}
