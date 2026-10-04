package com.example.baseballorders.simulator.domain.player.strategy;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.example.baseballorders.simulator.domain.player.strategy.batting.HighOnBaseHittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.batting.LongDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.batting.MiddleDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.batting.ShortDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BehaviorStrategiesTest {

    @Test
    @DisplayName("性格ごとの打撃生成メソッドは対応する打撃戦略クラスを返す")
    void createsHittingStrategyForEachPersonality() {
        // given
        var sut = SimulationRulesTestData.strategies();

        // when
        var shortDistance = sut.shortDistanceHittingStrategy();
        var middleDistance = sut.middleDistanceHittingStrategy();
        var longDistance = sut.longDistanceAtBat();
        var highOnBase = sut.highOnBaseHittingStrategy();

        // then
        assertAll(
                () -> assertInstanceOf(ShortDistanceHittingStrategy.class, shortDistance),
                () -> assertInstanceOf(MiddleDistanceHittingStrategy.class, middleDistance),
                () -> assertInstanceOf(LongDistanceHittingStrategy.class, longDistance),
                () -> assertInstanceOf(HighOnBaseHittingStrategy.class, highOnBase));
    }
}
