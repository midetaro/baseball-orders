package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilities;

/** 短距離バッター */
public final class ShortDistanceHittingStrategy implements HittingStrategy {

    private final BattingResultSelector selector;

    /**
     * 設定された打席確率で短距離打者の打撃を作成する。
     *
     * @param battingProbabilities 打席結果の判定に使う確率
     */
    public ShortDistanceHittingStrategy(BattingProbabilities battingProbabilities) {
        selector = new BattingResultSelector(battingProbabilities);
    }

    @Override
    public BattingResult batting(float onBasePercentage, float sluggish) {

        float random = RandomGenerator.nextFloat();
        float singleProbability = onBasePercentage * (1 - sluggish);
        float doubleProbability = onBasePercentage * sluggish;

        return selector.select(
                random, onBasePercentage, singleProbability, doubleProbability, 0, 0);
    }
}
