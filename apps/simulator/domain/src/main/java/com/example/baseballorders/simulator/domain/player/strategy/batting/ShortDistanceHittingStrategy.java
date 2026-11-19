package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilities;
import com.example.baseballorders.simulator.domain.rule.HittingDistribution;

/** 短距離バッター */
public final class ShortDistanceHittingStrategy implements HittingStrategy {

    private final BattingResultSelector selector;
    private final HittingDistribution distribution;

    /**
     * 設定された打席確率と安打配分で短距離打者の打撃を作成する。
     *
     * @param battingProbabilities 打席結果の判定に使う確率
     * @param distribution 安打種別の配分
     */
    public ShortDistanceHittingStrategy(
            BattingProbabilities battingProbabilities, HittingDistribution distribution) {
        selector = new BattingResultSelector(battingProbabilities);
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
