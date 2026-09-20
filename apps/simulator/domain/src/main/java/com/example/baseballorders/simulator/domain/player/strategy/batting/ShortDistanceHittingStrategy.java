package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;

/** 短距離バッター */
public final class ShortDistanceHittingStrategy implements HittingStrategy {

    @Override
    public BattingResult batting(float onBasePercentage, float sluggish) {

        float random = RandomGenerator.nextFloat();
        float singleProbability = onBasePercentage * (1 - sluggish);
        float doubleProbability = onBasePercentage * sluggish;

        if (random < singleProbability) {
            return BattingResult.HIT_SINGLE;
        } else if (random < singleProbability + doubleProbability) {
            return BattingResult.HIT_DOUBLE;
        }
        return BattingResult.OUT;
    }
}
