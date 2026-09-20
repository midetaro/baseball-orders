package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;

/** 中距離バッター */
public final class MiddleDistanceHittingStrategy implements HittingStrategy {

    public BattingResult batting(float onBasePercentage, float slugging) {
        float random = RandomGenerator.nextFloat();

        // 長打によって増えた塁数
        float extraBaseProbability = slugging - onBasePercentage;

        // 二塁打・三塁打・本塁打を同じ確率と仮定
        float doubleProbability = extraBaseProbability / 6;
        float tripleProbability = extraBaseProbability / 6;
        float homeRunProbability = extraBaseProbability / 6;

        float singleProbability = onBasePercentage - extraBaseProbability / 2;

        float cumulative = singleProbability;

        if (random < cumulative) {
            return BattingResult.HIT_SINGLE;
        }

        cumulative += doubleProbability;
        if (random < cumulative) {
            return BattingResult.HIT_DOUBLE;
        }

        cumulative += tripleProbability;
        if (random < cumulative) {
            return BattingResult.HIT_TRIPLE;
        }

        cumulative += homeRunProbability;
        if (random < cumulative) {
            return BattingResult.HIT_HOMER;
        }

        return BattingResult.OUT;
    }
}
