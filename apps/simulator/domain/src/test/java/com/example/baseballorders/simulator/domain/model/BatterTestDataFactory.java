package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.behavior.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.model.behavior.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.model.behavior.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.model.behavior.steal.StealStrategy;
import java.util.List;

public final class BatterTestDataFactory {

    private BatterTestDataFactory() {}

    static HittingStrategy hittingStrategy = BehaviorStrategies.middleDistanceAtBat();
    static StealStrategy eagerStealBehavior = BehaviorStrategies.eagerSteal();
    static StealStrategy nowayStealBehavior = BehaviorStrategies.noSteal();
    static BuntStrategy standardBuntStrategy = BehaviorStrategies.standardBunt();

    public static List<BatterEntity> mock() {
        return List.of(
                batter("batter1", 0.4f, 0.4f, eagerStealBehavior),
                batter("batter2", 0.4f, 0.2f, eagerStealBehavior),
                batter("batter3", 0.25f, 0.5f, eagerStealBehavior),
                batter("batter1", 0.2f, 0.7f, nowayStealBehavior),
                batter("batter2", 0.4f, 0.5f, eagerStealBehavior),
                batter("batter3", 0.3f, 0.3f, eagerStealBehavior),
                batter("batter1", 0.3f, 0.6f, eagerStealBehavior),
                batter("batter2", 0.3f, 0.4f, eagerStealBehavior),
                batter("batter3", 0.3f, 0.5f, eagerStealBehavior));
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
