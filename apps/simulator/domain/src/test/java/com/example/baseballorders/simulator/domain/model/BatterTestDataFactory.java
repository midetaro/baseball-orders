package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.model.behavior.AggressiveStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.MiddleStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.NowayStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.ShortDistanceAtBatBehavior;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import java.util.List;

public final class BatterTestDataFactory {

    private BatterTestDataFactory() {}

    static ShortDistanceAtBatBehavior shortDistanceAtBatBehavior = new ShortDistanceAtBatBehavior();
    static AggressiveStealBehavior aggressiveStealBehavior = new AggressiveStealBehavior();
    static MiddleStealBehavior middleStealBehavior = new MiddleStealBehavior();
    static NowayStealBehavior nowayStealBehavior = new NowayStealBehavior();

    public static List<BatterEntity> mock() {
        return List.of(
                new BatterEntity(
                        "batter1", 0.4f, 0.4f, shortDistanceAtBatBehavior, aggressiveStealBehavior),
                new BatterEntity(
                        "batter2", 0.4f, 0.2f, shortDistanceAtBatBehavior, aggressiveStealBehavior),
                new BatterEntity(
                        "batter3", 0.25f, 0.5f, shortDistanceAtBatBehavior, middleStealBehavior),
                new BatterEntity(
                        "batter1", 0.2f, 0.7f, shortDistanceAtBatBehavior, nowayStealBehavior),
                new BatterEntity(
                        "batter2", 0.4f, 0.5f, shortDistanceAtBatBehavior, middleStealBehavior),
                new BatterEntity(
                        "batter3", 0.3f, 0.3f, shortDistanceAtBatBehavior, middleStealBehavior),
                new BatterEntity(
                        "batter1", 0.3f, 0.6f, shortDistanceAtBatBehavior, middleStealBehavior),
                new BatterEntity(
                        "batter2", 0.3f, 0.4f, shortDistanceAtBatBehavior, middleStealBehavior),
                new BatterEntity(
                        "batter3", 0.3f, 0.5f, shortDistanceAtBatBehavior, middleStealBehavior));
    }
}
