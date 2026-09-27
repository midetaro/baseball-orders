package com.example.baseballorders.simulator.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.application.usecase.SimulationRunMode;
import com.example.baseballorders.simulator.domain.game.InningScript;
import com.example.baseballorders.simulator.domain.game.ScriptedBaseStateFactory;
import com.example.baseballorders.simulator.domain.player.BatterTestData;
import com.example.baseballorders.simulator.domain.player.strategy.Draws;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.statistics.GameTransition;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 1試合実行モードの決定論シナリオ。
 *
 * <p>実物: SimulateGameUseCase, GameBattingContext, InningStateContext, SingleGameTransitionObserver,
 * GameTransitionRecorder, GameStatisticsRecorder, ScoreAccumulator, BatterEntity, 各 Strategy
 *
 * <p>モック: BaseStateFactory / BasesState（{@code ScriptedBaseStateFactory} で1試合ぶんのイニング進行を脚本化）,
 * RandomGenerator（乱数列を固定）
 *
 * <p>担保する疎通: ユースケース(SINGLE_GAME_RUN) -> 設定された試合数を無視して1試合だけ実行 -> GameBattingContext ->
 * SingleGameTransitionObserver -> GameTransitionRecorder -> SimulationResult.transitions() の発生順・内容、
 * および試合終了通知 -> ScoreAccumulator -> SimulationResult.statistics()
 *
 * <p>担保しないもの: 走者配置ごとの進塁・得点規則（L2 の各 {@code BasesState} テストが担保する）、 SQS との疎通（infrastructure と
 * integration-test の担当）、バント・盗塁を含む推移の内容（{@code SingleGameTransitionObserverTest} が担保する）
 */
class SimulateGameUseCaseSingleGameScenarioTest {

    private static final int AT_BATS_PER_GAME = 27;

    /** 初回先頭打者だけ本塁打にし、残り26打席を三振にした乱数列を作る。 */
    private static float[] script() {
        return ScriptedRandom.concat(
                new float[] {Draws.HOMER},
                ScriptedRandom.repeat(Draws.STRIKEOUT, AT_BATS_PER_GAME - 1));
    }

    @Test
    @DisplayName("設定された試合数に関わらず1試合だけ実行し、発生順の状況推移と集計統計を返す")
    void runsExactlyOneGameAndReturnsTransitionsInOrder() {
        // given
        var factory =
                ScriptedBaseStateFactory.of(
                        InningScript.ofRunsPerInning(1, 0, 0, 0, 0, 0, 0, 0, 0));
        var sut = new SimulateGameUseCase(3, factory);

        // when
        SimulationResult result;
        try (ScriptedRandom random = ScriptedRandom.of(script())) {
            result =
                    sut.invoke(BatterTestData.swingOnlyLineUp(), SimulationRunMode.SINGLE_GAME_RUN);

            // then
            List<GameTransition> transitions = result.transitions();
            GameTransition firstTransition = transitions.get(0);
            GameTransition lastTransition = transitions.get(transitions.size() - 1);
            assertAll(
                    () -> assertEquals(1, result.statistics().gameCount(), "設定に関わらず1試合だけ実行すること"),
                    () -> assertEquals(1, result.statistics().homeRunCount()),
                    () -> assertEquals(AT_BATS_PER_GAME, transitions.size(), "全27打席ぶんの推移を記録すること"),
                    () -> assertEquals(1, firstTransition.inning()),
                    () -> assertEquals("本塁打", firstTransition.actionResult()),
                    () -> assertEquals(0, firstTransition.outCount(), "本塁打自身の遷移適用前のアウト数であること"),
                    () -> assertEquals(0, firstTransition.cumulativeScore(), "本塁打自身の加点前の累積得点であること"),
                    () -> assertEquals("走者なし", firstTransition.runnerState()),
                    () -> assertEquals(9, lastTransition.inning()),
                    () -> assertEquals("三振", lastTransition.actionResult()),
                    () -> assertEquals(2, lastTransition.outCount(), "最終打席自身の遷移適用前のアウト数であること"),
                    () -> assertEquals(1, lastTransition.cumulativeScore(), "1回に入った1点を反映していること"),
                    () -> assertEquals("走者なし", lastTransition.runnerState()),
                    () ->
                            assertEquals(
                                    AT_BATS_PER_GAME,
                                    random.consumedCount(),
                                    "1試合27打席ぶんの乱数を消費すること"),
                    () -> random.assertFullyConsumed(),
                    () -> factory.assertFullyPlayed());
        }
    }
}
