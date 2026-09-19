package com.example.baseballorders.simulator.domain.model.behavior.batting;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.springframework.stereotype.Component;

/** 長距離バッター */
@Component("longDistanceAtBat")
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
