package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StealStrategy;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import java.util.List;

public final class BatterTestDataFactory {

    static HittingStrategy hittingStrategy =
            SimulationRulesTestData.strategies().middleDistanceHittingStrategy();
    static StealStrategy eagerStealStrategy = SimulationRulesTestData.strategies().eagerSteal();
    static StealStrategy nowayStealBehavior = SimulationRulesTestData.strategies().noSteal();
    static BuntStrategy standardBuntStrategy = SimulationRulesTestData.strategies().standardBunt();

    private BatterTestDataFactory() {}

    public static List<BatterEntity> mock() {
        return List.of(
                batter("batter1", 0.4f, 0.4f, eagerStealStrategy),
                batter("batter2", 0.4f, 0.2f, eagerStealStrategy),
                batter("batter3", 0.25f, 0.5f, eagerStealStrategy),
                batter("batter1", 0.2f, 0.7f, nowayStealBehavior),
                batter("batter2", 0.4f, 0.5f, eagerStealStrategy),
                batter("batter3", 0.3f, 0.3f, eagerStealStrategy),
                batter("batter1", 0.3f, 0.6f, eagerStealStrategy),
                batter("batter2", 0.3f, 0.4f, eagerStealStrategy),
                batter("batter3", 0.3f, 0.5f, eagerStealStrategy));
    }

    private static BatterEntity batter(
            String name, float hitAverage, float slugging, StealStrategy stealStrategy) {
        return new BatterEntity(
                hitAverage,
                slugging,
                0.7f,
                0.8f,
                hittingStrategy,
                stealStrategy,
                standardBuntStrategy);
    }
}
