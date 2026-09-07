package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.springframework.stereotype.Component;

/** 中距離バッター */
@Component("middleDistanceAtBat")
public class MiddleDistanceBattingBehavior implements AtBatBehavior {

    public BattingResult batting(float hitAverage, float slugging) {
        float random = RandomGenerator.nextFloat();

        // 長打によって増えた塁数
        float extraBaseProbability = slugging - hitAverage;

        // 二塁打・三塁打・本塁打を同じ確率と仮定
        float doubleProbability = extraBaseProbability / 6;
        float tripleProbability = extraBaseProbability / 6;
        float homeRunProbability = extraBaseProbability / 6;

        float singleProbability = hitAverage - extraBaseProbability / 2;

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
