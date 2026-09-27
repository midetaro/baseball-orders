package com.example.baseballorders.simulator.domain.rule;

import com.example.baseballorders.simulator.domain.game.BaseStateFactory;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;

/**
 * 本番の {@code application.yml} と同じ確率設定をテストへ供給するフィクスチャ。
 *
 * <p>ドメインは確率の既定値を持たないため、テストも設定値を明示して組み立てる。値を変えた振る舞いを確認したいテストは、各設定の Staged Builder を直接使って独自の値を組み立てる。
 */
public final class SimulationRulesTestData {

    private SimulationRulesTestData() {}

    /**
     * 本番既定値と同じ確率設定を返す。
     *
     * @return 標準の確率設定
     */
    public static SimulationRules standard() {
        return SimulationRulesBuilder.simulationRules()
                .batting(
                        BattingProbabilitiesBuilder.battingProbabilities()
                                .walkProbability(0.05f)
                                .strikeoutProbabilityWhenNotOnBase(0.25f)
                                .build())
                .middleDistanceHitting(
                        HittingDistributionBuilder.hittingDistribution()
                                .doubleDivisor(6)
                                .tripleDivisor(6)
                                .homeRunDivisor(6)
                                .singleReductionDivisor(2)
                                .build())
                .longDistanceHitting(
                        HittingDistributionBuilder.hittingDistribution()
                                .doubleDivisor(8)
                                .tripleDivisor(8)
                                .homeRunDivisor(2)
                                .singleReductionDivisor(1)
                                .build())
                .standardSteal(
                        StealAttemptRatesBuilder.stealAttemptRates()
                                .toDoubleAttemptRate(0.2f)
                                .toTripleAttemptRate(0.05f)
                                .build())
                .eagerSteal(
                        StealAttemptRatesBuilder.stealAttemptRates()
                                .toDoubleAttemptRate(0.3f)
                                .toTripleAttemptRate(0.15f)
                                .build())
                .runnerAdvance(
                        RunnerAdvanceProbabilitiesBuilder.runnerAdvanceProbabilities()
                                .fromFirstProbability(0.2f)
                                .fromSecondProbability(0.2f)
                                .fromThirdProbability(0.1f)
                                .build())
                .build();
    }

    /**
     * 標準の確率設定を持つ塁Stateファクトリを返す。
     *
     * @return 標準設定のファクトリ
     */
    public static BaseStateFactory baseStateFactory() {
        return new BaseStateFactory(standard().runnerAdvance());
    }

    /**
     * 標準の確率設定を持つ行動戦略ファクトリを返す。
     *
     * @return 標準設定の戦略ファクトリ
     */
    public static BehaviorStrategies strategies() {
        return new BehaviorStrategies(standard());
    }
}
