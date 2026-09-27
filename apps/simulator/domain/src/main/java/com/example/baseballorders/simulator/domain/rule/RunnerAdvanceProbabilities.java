package com.example.baseballorders.simulator.domain.rule;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * 打撃による凡退で先頭走者が進塁する、走者のいる塁ごとの確率設定。
 *
 * @param fromFirstProbability 一塁走者が二塁へ進む確率
 * @param fromSecondProbability 二塁走者が三塁へ進む確率
 * @param fromThirdProbability 三塁走者が生還する確率
 */
@Builder(style = BuilderStyle.STAGED)
public record RunnerAdvanceProbabilities(
        float fromFirstProbability, float fromSecondProbability, float fromThirdProbability) {}
