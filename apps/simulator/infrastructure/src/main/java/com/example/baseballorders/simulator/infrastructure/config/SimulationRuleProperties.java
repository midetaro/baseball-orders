package com.example.baseballorders.simulator.infrastructure.config;

import com.example.baseballorders.simulator.domain.rule.BattingProbabilitiesBuilder;
import com.example.baseballorders.simulator.domain.rule.HittingDistribution;
import com.example.baseballorders.simulator.domain.rule.HittingDistributionBuilder;
import com.example.baseballorders.simulator.domain.rule.RunnerAdvanceProbabilitiesBuilder;
import com.example.baseballorders.simulator.domain.rule.SimulationRules;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesBuilder;
import com.example.baseballorders.simulator.domain.rule.StealAttemptRates;
import com.example.baseballorders.simulator.domain.rule.StealAttemptRatesBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code simulation.rule} 配下の確率設定を束縛し、ドメインの設定値オブジェクトへ変換する。
 *
 * @param batting 打席結果の判定に使う確率
 * @param middleDistanceHitting 中距離打者の長打配分
 * @param longDistanceHitting 長距離打者の長打配分
 * @param standardSteal 標準盗塁戦略の企図率
 * @param eagerSteal 積極盗塁戦略の企図率
 * @param runnerAdvance 凡退時の走者進塁確率
 */
@ConfigurationProperties(prefix = "simulation.rule")
public record SimulationRuleProperties(
        Batting batting,
        Hitting middleDistanceHitting,
        Hitting longDistanceHitting,
        Steal standardSteal,
        Steal eagerSteal,
        RunnerAdvance runnerAdvance) {

    /**
     * 打者の成績に依存しない打席確率。
     *
     * @param walkProbability 四球となる確率の上限
     * @param strikeoutProbabilityWhenNotOnBase 出塁しなかった打席のうち三振になる割合
     */
    public record Batting(float walkProbability, float strikeoutProbabilityWhenNotOnBase) {}

    /**
     * 長打によって増えた塁数を安打種別へ配分する除数。
     *
     * @param doubleDivisor 二塁打の重みを求める除数
     * @param tripleDivisor 三塁打の重みを求める除数
     * @param homeRunDivisor 本塁打の重みを求める除数
     * @param singleReductionDivisor 単打の重みから差し引く量を求める除数
     */
    public record Hitting(
            float doubleDivisor,
            float tripleDivisor,
            float homeRunDivisor,
            float singleReductionDivisor) {}

    /**
     * 盗塁を企図する割合。
     *
     * @param toDoubleAttemptRate 一塁走者が二塁を狙う割合
     * @param toTripleAttemptRate 二塁走者が三塁を狙う割合
     */
    public record Steal(float toDoubleAttemptRate, float toTripleAttemptRate) {}

    /**
     * 凡退時に先頭走者が進塁する確率。
     *
     * @param fromFirstProbability 一塁走者が二塁へ進む確率
     * @param fromSecondProbability 二塁走者が三塁へ進む確率
     * @param fromThirdProbability 三塁走者が生還する確率
     */
    public record RunnerAdvance(
            float fromFirstProbability, float fromSecondProbability, float fromThirdProbability) {}

    /**
     * 束縛した設定値をドメインの確率設定へ変換する。
     *
     * @return ドメインが使用する確率設定
     */
    public SimulationRules toSimulationRules() {
        return SimulationRulesBuilder.simulationRules()
                .batting(
                        BattingProbabilitiesBuilder.battingProbabilities()
                                .walkProbability(batting.walkProbability())
                                .strikeoutProbabilityWhenNotOnBase(
                                        batting.strikeoutProbabilityWhenNotOnBase())
                                .build())
                .middleDistanceHitting(hittingDistribution(middleDistanceHitting))
                .longDistanceHitting(hittingDistribution(longDistanceHitting))
                .standardSteal(stealAttemptRates(standardSteal))
                .eagerSteal(stealAttemptRates(eagerSteal))
                .runnerAdvance(
                        RunnerAdvanceProbabilitiesBuilder.runnerAdvanceProbabilities()
                                .fromFirstProbability(runnerAdvance.fromFirstProbability())
                                .fromSecondProbability(runnerAdvance.fromSecondProbability())
                                .fromThirdProbability(runnerAdvance.fromThirdProbability())
                                .build())
                .build();
    }

    private static HittingDistribution hittingDistribution(Hitting hitting) {
        return HittingDistributionBuilder.hittingDistribution()
                .doubleDivisor(hitting.doubleDivisor())
                .tripleDivisor(hitting.tripleDivisor())
                .homeRunDivisor(hitting.homeRunDivisor())
                .singleReductionDivisor(hitting.singleReductionDivisor())
                .build();
    }

    private static StealAttemptRates stealAttemptRates(Steal steal) {
        return StealAttemptRatesBuilder.stealAttemptRates()
                .toDoubleAttemptRate(steal.toDoubleAttemptRate())
                .toTripleAttemptRate(steal.toTripleAttemptRate())
                .build();
    }
}
