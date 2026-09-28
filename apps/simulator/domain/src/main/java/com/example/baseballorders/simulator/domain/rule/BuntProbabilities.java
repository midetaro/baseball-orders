package com.example.baseballorders.simulator.domain.rule;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * バント戦略が用いる成功率・企図率の設定。
 *
 * @param advancingSuccessRate 進塁バントの成功率
 * @param squeezeSuccessRate スクイズの成功率
 * @param squeezeChallengeRate スクイズを試みる（企図する）割合
 */
@Builder(style = BuilderStyle.STAGED)
public record BuntProbabilities(
        float advancingSuccessRate, float squeezeSuccessRate, float squeezeChallengeRate) {}
