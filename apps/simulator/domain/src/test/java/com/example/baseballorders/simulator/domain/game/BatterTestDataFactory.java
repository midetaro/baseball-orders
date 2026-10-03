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
                batter("batter1", 0.4f, eagerStealStrategy),
                batter("batter2", 0.4f, eagerStealStrategy),
                batter("batter3", 0.25f, eagerStealStrategy),
                batter("batter1", 0.2f, nowayStealBehavior),
                batter("batter2", 0.4f, eagerStealStrategy),
                batter("batter3", 0.3f, eagerStealStrategy),
                batter("batter1", 0.3f, eagerStealStrategy),
                batter("batter2", 0.3f, eagerStealStrategy),
                batter("batter3", 0.3f, eagerStealStrategy));
    }

    private static BatterEntity batter(String name, float hitAverage, StealStrategy stealStrategy) {
        return new BatterEntity(hitAverage, hittingStrategy, stealStrategy, standardBuntStrategy);
    }
}
