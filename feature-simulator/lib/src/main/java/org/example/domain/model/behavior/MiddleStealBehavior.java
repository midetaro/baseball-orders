package org.example.domain.model.behavior;

import org.example.domain.code.StealResult;

public class MiddleStealBehavior implements StealStrategy {

    private final float TRY_AVERAGE = 0.2f;
    private final float NOT_TRY = 1 - TRY_AVERAGE;
    private final float SUCCESS_PROBABILITY = NOT_TRY + 0.8f * TRY_AVERAGE;

    @Override
    public StealResult runToDouble() {
        float random = (float) Math.random();

        if (random < NOT_TRY) {
            return StealResult.NOT_TRY;
        } else if (NOT_TRY < random && random < SUCCESS_PROBABILITY) {
            return StealResult.SUCCESS;
        } else {
            return StealResult.FAILURE;
        }
    }

    private final float TO_TRIPLE_TRY_AVERAGE = 0.05f;
    private final float TO_TRIPLE_NOT_TRY = 1 - TO_TRIPLE_TRY_AVERAGE;
    private final float TO_TRIPLE_SUCCESS_PROBABILITY = TO_TRIPLE_NOT_TRY + 0.9f * TO_TRIPLE_TRY_AVERAGE;

    @Override
    public StealResult runToTriple() {
        float random = (float) Math.random();
        if (random < TO_TRIPLE_TRY_AVERAGE) {
            return StealResult.NOT_TRY;
        } else if (TO_TRIPLE_NOT_TRY < random && random < TO_TRIPLE_SUCCESS_PROBABILITY) {
            return StealResult.SUCCESS;
        } else {
            return StealResult.FAILURE;
        }
    }
}
