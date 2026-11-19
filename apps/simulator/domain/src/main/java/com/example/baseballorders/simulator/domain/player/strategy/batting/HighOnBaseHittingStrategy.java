package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilities;
import com.example.baseballorders.simulator.domain.rule.HittingDistribution;

/** 高出塁率バッター。打率は変えず、四球確率と安打配分を専用の設定値にする。 */
public final class HighOnBaseHittingStrategy implements HittingStrategy {

    private final BattingResultSelector selector;
    private final HittingDistribution distribution;

    /**
     * 高出塁率打者用の打席確率と長打配分で高出塁率打者の打撃を作成する。
     *
     * @param highOnBaseBatting 高出塁率打者用の四球確率を持つ打席確率
     * @param distribution 安打種別の配分
     */
    public HighOnBaseHittingStrategy(
            BattingProbabilities highOnBaseBatting, HittingDistribution distribution) {
        selector = new BattingResultSelector(highOnBaseBatting);
        this.distribution = distribution;
    }

    @Override
    public BattingResult batting(float battingAverage) {
        float random = RandomGenerator.nextFloat();

        // 安打確率は打率で決まり、安打種別は設定された重みの比率で配分する
        return selector.select(
                random,
                battingAverage,
                distribution.singleWeight(),
                distribution.doubleWeight(),
                distribution.tripleWeight(),
                distribution.homeRunWeight());
    }
}
