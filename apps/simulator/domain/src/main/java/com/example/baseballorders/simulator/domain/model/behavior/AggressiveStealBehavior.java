package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.springframework.stereotype.Component;

@Component("aggressiveStealBehavior")
public class AggressiveStealBehavior implements StealStrategy {

    private final float TO_DOUBLE_TRY_AVERAGE = 0.3f;
    private final float TO_DOUBLE_NOT_TRY = 1 - TO_DOUBLE_TRY_AVERAGE;
    private final float TO_DOUBLE_SUCCESS_PROBABILITY =
            TO_DOUBLE_NOT_TRY + 0.9f * TO_DOUBLE_TRY_AVERAGE;

    @Override
    public StealResult runToDouble() {
        float random = RandomGenerator.nextFloat();
        System.out.println(
                "TO_DOUBLE_NOT_TRY:"
                        + TO_DOUBLE_NOT_TRY
                        + " TO_DOUBLE_SUCCESS_PROBABILITY:"
                        + TO_DOUBLE_SUCCESS_PROBABILITY
                        + " random:"
                        + random);
        if (random < TO_DOUBLE_NOT_TRY) {
            return StealResult.NOT_TRY;
        } else if (TO_DOUBLE_NOT_TRY < random && random < TO_DOUBLE_SUCCESS_PROBABILITY) {
            return StealResult.SUCCESS;
        } else {
            return StealResult.FAILURE;
        }
    }

    private final float TO_TRIPLE_TRY_AVERAGE = 0.15f;
    private final float TO_TRIPLE_NOT_TRY = 1 - TO_TRIPLE_TRY_AVERAGE;
    private final float TO_TRIPLE_SUCCESS_PROBABILITY =
            TO_TRIPLE_NOT_TRY + 0.9f * TO_TRIPLE_TRY_AVERAGE;

    @Override
    public StealResult runToTriple() {
        float random = RandomGenerator.nextFloat();
        if (random < TO_TRIPLE_NOT_TRY) {
            return StealResult.NOT_TRY;
        } else if (TO_TRIPLE_NOT_TRY < random && random < TO_TRIPLE_SUCCESS_PROBABILITY) {
            return StealResult.SUCCESS;
        } else {
            return StealResult.FAILURE;
        }
    }
}
