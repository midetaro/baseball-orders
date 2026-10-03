package com.example.baseballorders.simulator.infrastructure.config;

import com.example.baseballorders.simulator.domain.rule.BattingProbabilitiesBuilder;
import com.example.baseballorders.simulator.domain.rule.BuntProbabilities;
import com.example.baseballorders.simulator.domain.rule.BuntProbabilitiesBuilder;
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
 * @param shortDistanceHitting 短距離打者の安打配分
 * @param middleDistanceHitting 中距離打者の長打配分
 * @param longDistanceHitting 長距離打者の長打配分
 * @param highOnBaseHitting 高出塁率打者の安打配分
 * @param highOnBaseBatting 高出塁率打者の打席確率
 * @param standardSteal 標準盗塁戦略の企図率
 * @param eagerSteal 積極盗塁戦略の企図率
 * @param runnerAdvance 凡退時の走者進塁確率
 * @param stealSuccessRate 盗塁成功率
 * @param bunt バント戦略の成功率・企図率
 */
@ConfigurationProperties(prefix = "simulation.rule")
public record SimulationRuleProperties(
        Batting batting,
        Hitting shortDistanceHitting,
        Hitting middleDistanceHitting,
        Hitting longDistanceHitting,
        Hitting highOnBaseHitting,
        HighOnBaseBatting highOnBaseBatting,
        Steal standardSteal,
        Steal eagerSteal,
        RunnerAdvance runnerAdvance,
        float stealSuccessRate,
        Bunt bunt) {

    /**
     * 打者の成績に依存しない打席確率。
     *
     * @param walkProbability 四球となる確率の上限
     * @param strikeoutProbabilityWhenNotOnBase 出塁しなかった打席のうち三振になる割合
     */
    public record Batting(float walkProbability, float strikeoutProbabilityWhenNotOnBase) {}

    /**
     * 高出塁率打者の打席確率。打率は変えず、四球確率だけを標準と別の値にする。
     *
     * @param walkProbability 高出塁率打者が四球となる確率
     */
    public record HighOnBaseBatting(float walkProbability) {}

    /**
     * 安打を各安打種別へ配分する相対的な重み。合計が1である必要はなく、0の種別は発生しない。
     *
     * @param singleWeight 単打の重み
     * @param doubleWeight 二塁打の重み
     * @param tripleWeight 三塁打の重み
     * @param homeRunWeight 本塁打の重み
     */
    public record Hitting(
            float singleWeight, float doubleWeight, float tripleWeight, float homeRunWeight) {}

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
     * バント戦略が用いる成功率・企図率。
     *
     * @param advancingSuccessRate 進塁バントの成功率
     * @param squeezeSuccessRate スクイズの成功率
     * @param squeezeChallengeRate スクイズを試みる（企図する）割合
     */
    public record Bunt(
            float advancingSuccessRate, float squeezeSuccessRate, float squeezeChallengeRate) {}

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
                .shortDistanceHitting(hittingDistribution(shortDistanceHitting))
                .middleDistanceHitting(hittingDistribution(middleDistanceHitting))
                .longDistanceHitting(hittingDistribution(longDistanceHitting))
                .highOnBaseHitting(hittingDistribution(highOnBaseHitting))
                .highOnBaseWalkProbability(highOnBaseBatting.walkProbability())
                .standardSteal(stealAttemptRates(standardSteal))
                .eagerSteal(stealAttemptRates(eagerSteal))
                .runnerAdvance(
                        RunnerAdvanceProbabilitiesBuilder.runnerAdvanceProbabilities()
                                .fromFirstProbability(runnerAdvance.fromFirstProbability())
                                .fromSecondProbability(runnerAdvance.fromSecondProbability())
                                .fromThirdProbability(runnerAdvance.fromThirdProbability())
                                .build())
                .stealSuccessRate(stealSuccessRate)
                .buntProbabilities(buntProbabilities(bunt))
                .build();
    }

    private static HittingDistribution hittingDistribution(Hitting hitting) {
        return HittingDistributionBuilder.hittingDistribution()
                .singleWeight(hitting.singleWeight())
                .doubleWeight(hitting.doubleWeight())
                .tripleWeight(hitting.tripleWeight())
                .homeRunWeight(hitting.homeRunWeight())
                .build();
    }

    private static StealAttemptRates stealAttemptRates(Steal steal) {
        return StealAttemptRatesBuilder.stealAttemptRates()
                .toDoubleAttemptRate(steal.toDoubleAttemptRate())
                .toTripleAttemptRate(steal.toTripleAttemptRate())
                .build();
    }

    private static BuntProbabilities buntProbabilities(Bunt bunt) {
        return BuntProbabilitiesBuilder.buntProbabilities()
                .advancingSuccessRate(bunt.advancingSuccessRate())
                .squeezeSuccessRate(bunt.squeezeSuccessRate())
                .squeezeChallengeRate(bunt.squeezeChallengeRate())
                .build();
    }
}
