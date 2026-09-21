package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;

/** 長距離バッター */
public final class LongDistanceHittingStrategy implements HittingStrategy {

    @Override
    public BattingResult batting(float onBasePercentage, float slugging) {
        float random = RandomGenerator.nextFloat();

        // 長打によって増えた塁数
        float extraBaseProbability = slugging - onBasePercentage;

        // 中距離バッターより本塁打へ配分を寄せ、単打を減らしてアウト率も上げる
        float doubleProbability = extraBaseProbability / 8;
        float tripleProbability = extraBaseProbability / 8;
        float homeRunProbability = extraBaseProbability / 2;

        float singleProbability = onBasePercentage - extraBaseProbability;

        return BattingResultSelector.select(
                random,
                onBasePercentage,
                singleProbability,
                doubleProbability,
                tripleProbability,
                homeRunProbability);
    }
}
