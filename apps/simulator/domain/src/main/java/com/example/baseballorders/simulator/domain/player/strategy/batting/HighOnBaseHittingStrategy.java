package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilities;
import com.example.baseballorders.simulator.domain.rule.HittingDistribution;

/** 高出塁率バッター。打率は変えず、四球確率だけを専用の設定値にする。 */
public final class HighOnBaseHittingStrategy implements HittingStrategy {

    private final BattingResultSelector selector;
    private final HittingDistribution distribution;

    /**
     * 高出塁率打者用の打席確率と長打配分で高出塁率打者の打撃を作成する。
     *
     * @param highOnBaseBatting 高出塁率打者用の四球確率を持つ打席確率
     * @param distribution 長打によって増えた塁数の配分
     */
    public HighOnBaseHittingStrategy(
            BattingProbabilities highOnBaseBatting, HittingDistribution distribution) {
        selector = new BattingResultSelector(highOnBaseBatting);
        this.distribution = distribution;
    }

    @Override
    public BattingResult batting(float battingAverage, float slugging) {
        float random = RandomGenerator.nextFloat();

        // 長打によって増えた塁数
        float extraBaseProbability = slugging - battingAverage;

        // 安打の配分は設定された除数に従い、四球確率だけが標準と異なる
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
