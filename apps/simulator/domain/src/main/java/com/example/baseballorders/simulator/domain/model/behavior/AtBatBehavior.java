package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BattingResult;

public interface AtBatBehavior {

    /**
     * 打者の打撃成績と戦略に基づいて、一打席の打撃結果を決定する。
     *
     * @param hitAverage 打者の打率
     * @param sluggish 打者の長打率
     * @return 一打席の打撃結果
     */
    BattingResult batting(float hitAverage, float sluggish);
}
