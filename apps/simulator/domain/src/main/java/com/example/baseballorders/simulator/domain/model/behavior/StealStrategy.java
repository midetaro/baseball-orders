package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.StealResult;

public interface StealStrategy {

    /**
     * Determines an attempted steal from first base to second base.
     *
     * @param successRate success probability for the runner
     * @return whether the steal was attempted and its result
     */
    StealResult runToDouble(float successRate);

    /**
     * Determines an attempted steal from second base to third base.
     *
     * @param successRate success probability for the runner
     * @return whether the steal was attempted and its result
     */
    StealResult runToTriple(float successRate);
}
