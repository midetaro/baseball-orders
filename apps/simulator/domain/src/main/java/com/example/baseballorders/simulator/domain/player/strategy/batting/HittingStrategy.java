package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;

public sealed interface HittingStrategy
        permits HighOnBaseHittingStrategy,
                LongDistanceHittingStrategy,
                MiddleDistanceHittingStrategy,
                ShortDistanceHittingStrategy {

    /**
     * 打者の打撃成績と戦略に基づいて、一打席の打撃結果を決定する。四球確率は打率とは独立した設定値を使う。
     *
     * @param battingAverage 四球を除く打数に対する安打の割合（打率）
     * @return 一打席の打撃結果
     */
    BattingResult batting(float battingAverage);
}
