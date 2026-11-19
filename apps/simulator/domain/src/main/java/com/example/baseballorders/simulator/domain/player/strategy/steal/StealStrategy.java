package com.example.baseballorders.simulator.domain.player.strategy.steal;

import com.example.baseballorders.simulator.domain.play.StealResult;

public sealed interface StealStrategy
        permits EagerStealStrategy, StandardStealStrategy, NowayStealStrategy {

    /**
     * Determines an attempted steal from first base to second base.
     *
     * @return whether the steal was attempted and its result
     */
    StealResult runToDouble();

    /**
     * Determines an attempted steal from second base to third base.
     *
     * @return whether the steal was attempted and its result
     */
    StealResult runToTriple();
}
