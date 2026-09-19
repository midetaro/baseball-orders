package com.example.baseballorders.simulator.domain.entity.behavior;

import com.example.baseballorders.simulator.domain.entity.behavior.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.batting.LongDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.batting.MiddleDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.bunt.EagerBuntStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.bunt.NowayBuntStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.bunt.StandardBuntStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.steal.EagerStealStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.steal.NowayStealStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.steal.StealStrategy;

/** Provides behavior strategy instances without exposing their concrete implementations. */
public final class BehaviorStrategies {

    private BehaviorStrategies() {}

    /**
     * Creates the middle-distance batting behavior.
     *
     * @return a middle-distance batting behavior
     */
    public static HittingStrategy middleDistanceAtBat() {
        return new MiddleDistanceHittingStrategy();
    }

    /**
     * Creates the long-distance batting behavior.
     *
     * @return a long-distance batting behavior
     */
    public static HittingStrategy longDistanceAtBat() {
        return new LongDistanceHittingStrategy();
    }

    /**
     * Creates the eager stealing behavior.
     *
     * @return an eager stealing behavior
     */
    public static StealStrategy eagerSteal() {
        return new EagerStealStrategy();
    }

    /**
     * Creates the stealing behavior that never attempts a steal.
     *
     * @return a no-steal behavior
     */
    public static StealStrategy noSteal() {
        return new NowayStealStrategy();
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
