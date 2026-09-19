package com.example.baseballorders.simulator.domain.model.behavior.batting;

import com.example.baseballorders.simulator.domain.code.BattingResult;

public sealed interface HittingStrategy
        permits LongDistanceHittingStrategy,
                MiddleDistanceHittingStrategy,
                ShortDistanceHittingStrategy {

    /**
     * 打者の打撃成績と戦略に基づいて、一打席の打撃結果を決定する。
     *
     * @param onBasePercentage 打者の出塁率
     * @param sluggish 打者の長打率
     * @return 一打席の打撃結果
     */
    BattingResult batting(float onBasePercentage, float sluggish);
}
