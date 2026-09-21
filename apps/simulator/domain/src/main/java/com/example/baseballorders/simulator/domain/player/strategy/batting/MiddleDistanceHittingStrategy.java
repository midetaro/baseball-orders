package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;

/** 中距離バッター */
public final class MiddleDistanceHittingStrategy implements HittingStrategy {

    @Override
    public BattingResult batting(float onBasePercentage, float slugging) {
        float random = RandomGenerator.nextFloat();

        // 長打によって増えた塁数
        float extraBaseProbability = slugging - onBasePercentage;

        // 二塁打・三塁打・本塁打を同じ確率と仮定
        float doubleProbability = extraBaseProbability / 6;
        float tripleProbability = extraBaseProbability / 6;
        float homeRunProbability = extraBaseProbability / 6;

        float singleProbability = onBasePercentage - extraBaseProbability / 2;

        return BattingResultSelector.select(
                random,
                onBasePercentage,
                singleProbability,
                doubleProbability,
                tripleProbability,
                homeRunProbability);
    }
}
