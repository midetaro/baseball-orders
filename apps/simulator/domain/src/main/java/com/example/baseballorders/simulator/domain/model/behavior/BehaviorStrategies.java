package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.model.behavior.batting.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.batting.LongDistanceBattingBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.batting.MiddleDistanceBattingBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.model.behavior.bunt.EagerBuntStrategy;
import com.example.baseballorders.simulator.domain.model.behavior.bunt.NowayBuntStrategy;
import com.example.baseballorders.simulator.domain.model.behavior.bunt.StandardBuntStrategy;
import com.example.baseballorders.simulator.domain.model.behavior.steal.EagerStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.steal.NowayStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.steal.StealStrategy;

/** Provides behavior strategy instances without exposing their concrete implementations. */
public final class BehaviorStrategies {

    private BehaviorStrategies() {}

    /**
     * Creates the middle-distance batting behavior.
     *
     * @return a middle-distance batting behavior
     */
    public static AtBatBehavior middleDistanceAtBat() {
        return new MiddleDistanceBattingBehavior();
    }

    /**
     * Creates the long-distance batting behavior.
     *
     * @return a long-distance batting behavior
     */
    public static AtBatBehavior longDistanceAtBat() {
        return new LongDistanceBattingBehavior();
    }

    /**
     * Creates the eager stealing behavior.
     *
     * @return an eager stealing behavior
     */
    public static StealStrategy eagerSteal() {
        return new EagerStealBehavior();
    }

    /**
     * Creates the stealing behavior that never attempts a steal.
     *
     * @return a no-steal behavior
     */
    public static StealStrategy noSteal() {
        return new NowayStealBehavior();
    }

    /**
     * Creates the standard bunt strategy.
     *
     * @return a standard bunt strategy
     */
    public static BuntStrategy standardBunt() {
        return new StandardBuntStrategy();
    }

    /**
     * Creates the eager bunt strategy.
     *
     * @return an eager bunt strategy
     */
    public static BuntStrategy eagerBunt() {
        return new EagerBuntStrategy();
    }

    /**
     * Creates the bunt strategy that never attempts a bunt.
     *
     * @return a no-bunt strategy
     */
    public static BuntStrategy noBunt() {
        return new NowayBuntStrategy();
    }
}
