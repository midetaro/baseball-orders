package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilities;
import com.example.baseballorders.simulator.domain.rule.HittingDistribution;

/** 長距離バッター */
public final class LongDistanceHittingStrategy implements HittingStrategy {

    private final BattingResultSelector selector;
    private final HittingDistribution distribution;

    /**
     * 設定された打席確率と長打配分で長距離打者の打撃を作成する。
     *
     * @param battingProbabilities 打席結果の判定に使う確率
     * @param distribution 長打によって増えた塁数の配分
     */
    public LongDistanceHittingStrategy(
            BattingProbabilities battingProbabilities, HittingDistribution distribution) {
        selector = new BattingResultSelector(battingProbabilities);
        this.distribution = distribution;
    }

    @Override
    public BattingResult batting(float battingAverage, float slugging) {
        float random = RandomGenerator.nextFloat();

        // 長打によって増えた塁数
        float extraBaseProbability = slugging - battingAverage;

        // 中距離バッターより本塁打へ配分を寄せ、単打を減らす設定を使う
        float doubleProbability = extraBaseProbability / distribution.doubleDivisor();
        float tripleProbability = extraBaseProbability / distribution.tripleDivisor();
        float homeRunProbability = extraBaseProbability / distribution.homeRunDivisor();

        float singleProbability =
                battingAverage - extraBaseProbability / distribution.singleReductionDivisor();

        return selector.select(
                random,
                battingAverage,
                singleProbability,
                doubleProbability,
                tripleProbability,
                homeRunProbability);
    }
}
