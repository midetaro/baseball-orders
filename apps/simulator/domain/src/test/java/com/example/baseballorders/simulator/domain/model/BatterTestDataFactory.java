package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.model.behavior.EagerStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.MiddleStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.NowayStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.ShortDistanceAtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.StandardBuntStrategy;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import java.util.List;

public final class BatterTestDataFactory {

    private BatterTestDataFactory() {}

    static ShortDistanceAtBatBehavior shortDistanceAtBatBehavior = new ShortDistanceAtBatBehavior();
    static EagerStealBehavior eagerStealBehavior = new EagerStealBehavior();
    static MiddleStealBehavior middleStealBehavior = new MiddleStealBehavior();
    static NowayStealBehavior nowayStealBehavior = new NowayStealBehavior();
    static StandardBuntStrategy standardBuntStrategy = new StandardBuntStrategy();

    public static List<BatterEntity> mock() {
        return List.of(
                batter("batter1", 0.4f, 0.4f, eagerStealBehavior),
                batter("batter2", 0.4f, 0.2f, eagerStealBehavior),
                batter("batter3", 0.25f, 0.5f, middleStealBehavior),
                batter("batter1", 0.2f, 0.7f, nowayStealBehavior),
                batter("batter2", 0.4f, 0.5f, middleStealBehavior),
                batter("batter3", 0.3f, 0.3f, middleStealBehavior),
                batter("batter1", 0.3f, 0.6f, middleStealBehavior),
                batter("batter2", 0.3f, 0.4f, middleStealBehavior),
                batter("batter3", 0.3f, 0.5f, middleStealBehavior));
    }

    private static BatterEntity batter(
            String name, float hitAverage, float slugging, StealStrategy stealStrategy) {
        return new BatterEntity(
                name,
                hitAverage,
                slugging,
                0.7f,
                shortDistanceAtBatBehavior,
                stealStrategy,
                standardBuntStrategy);
    }
}
