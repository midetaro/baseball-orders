package com.example.baseballorders.simulator.domain.rule;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * シミュレーションの確率設定をまとめて戦略とStateへ供給する設定の集合。
 *
 * <p>値は外部設定から供給される。ドメインは既定値を持たない。
 *
 * @param batting 打席結果の判定に使う確率
 * @param middleDistanceHitting 中距離打者の長打配分
 * @param longDistanceHitting 長距離打者の長打配分
 * @param standardSteal 標準盗塁戦略の企図率
 * @param eagerSteal 積極盗塁戦略の企図率
 * @param runnerAdvance 凡退時の走者進塁確率
 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationRules(
        BattingProbabilities batting,
        HittingDistribution middleDistanceHitting,
        HittingDistribution longDistanceHitting,
        StealAttemptRates standardSteal,
        StealAttemptRates eagerSteal,
        RunnerAdvanceProbabilities runnerAdvance) {}
