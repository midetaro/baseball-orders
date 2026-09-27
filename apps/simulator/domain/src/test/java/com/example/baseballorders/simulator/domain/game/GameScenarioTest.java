package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.simulator.domain.player.BatterTestData;
import com.example.baseballorders.simulator.domain.player.strategy.Draws;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsBuilder;
import com.example.baseballorders.simulator.domain.statistics.StatisticsAssertions;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 試合レベルのシナリオ。
 *
 * <p>実物: GameBattingContext, InningStateContext, InningState, GameStatisticsRecorder, BatterEntity,
 * 各 Strategy
 *
 * <p>モック: BaseStateFactory / BasesState（{@code ScriptedBaseStateFactory} でイニング進行を脚本化）,
 * RandomGenerator（乱数列を固定）
 *
 * <p>担保する疎通: 打席 -> イニング完了 -> 試合得点の合算 -> 9 回での試合終了 -> GameCompletionObserver への 1 回の通知、 および 1
 * 打席ごとの打撃結果が試合統計へ積まれること
 *
 * <p>担保しないもの: 走者配置ごとの進塁・得点規則（L2 の各 {@code BasesState} テストが担保する）、 盗塁・バントフェーズ（フェイクは走者を持たないため能力を露出しない）
 */
class GameScenarioTest {

    private static GameStatistics soloHomeRunStatistics(int homeRuns) {
        return GameStatisticsBuilder.gameStatistics()
                .hitCount(homeRuns)
                .singleHitCount(0)
                .doubleHitCount(0)
                .tripleHitCount(0)
                .homeRunCount(homeRuns)
                .soloHomeRunCount(homeRuns)
                .twoRunHomeRunCount(0)
                .threeRunHomeRunCount(0)
                .grandSlamCount(0)
                .buntCount(0)
                .stealCount(0)
                .buntFailureCount(0)
                .stealFailureCount(0)
                .advancingBuntCount(0)
                .squeezeBuntCount(0)
                .advancingBuntFailureCount(0)
                .squeezeBuntFailureCount(0)
                .stealToSecondCount(0)
                .stealToThirdCount(0)
                .build();
    }

    @Test
    @DisplayName("脚本どおりに9イニングを終えると得点を合算し完了通知を一度だけ行う")
    void completesNineInningsAndNotifiesOnce() {
        // given
        var script = InningScript.ofRunsPerInning(2, 0, 0, 1, 0, 0, 0, 0, 1);
        var factory = ScriptedBaseStateFactory.of(script);
        AtomicInteger notificationCount = new AtomicInteger();
        AtomicLong notifiedScore = new AtomicLong();
        AtomicReference<GameStatistics> notifiedStatistics = new AtomicReference<>();
        var sut =
                new GameBattingContext(
                        BatterTestData.swingOnlyLineUp(),
                        (totalScore, gameStatistics) -> {
                            notificationCount.incrementAndGet();
                            notifiedScore.set(totalScore);
                            notifiedStatistics.set(gameStatistics);
                        },
                        factory);

        // when
        try (ScriptedRandom random =
                ScriptedRandom.repeating(Draws.STRIKEOUT, script.totalAtBats())) {
            while (!sut.isGameOver()) {
                sut.nextAtBat();
            }

            // then
            assertAll(
                    () -> assertTrue(sut.isGameOver(), "9回で試合が終了すること"),
                    () -> assertEquals(9, sut.getInning(), "10回目へ進まないこと"),
                    () -> assertEquals(4, sut.getTotalScore(), "各イニング得点の総和になること"),
                    () -> assertEquals(script.totalRuns(), sut.getTotalScore()),
                    () -> assertEquals(1, notificationCount.get(), "完了通知はちょうど1回であること"),
                    () -> assertEquals(4, notifiedScore.get(), "通知される得点が最終得点であること"),
                    () -> assertEquals(soloHomeRunStatistics(0), notifiedStatistics.get()),
                    () -> assertEquals(27, random.consumedCount(), "1打席あたり乱数は打撃の1個だけであること"),
                    () -> random.assertFullyConsumed(),
                    () -> factory.assertFullyPlayed(),
                    () -> assertEquals(1, factory.startedGameCount(), "脚本を使った試合は1件であること"));
        }
    }

    @Test
    @DisplayName("試合終了後の打席とイニング終了要求は無視され得点も通知も変わらない")
    void ignoresEventsAfterGameOver() {
        // given
        var script = InningScript.ofRunsPerInning(1, 0, 0, 0, 0, 0, 0, 0, 0);
        var factory = ScriptedBaseStateFactory.of(script);
        AtomicInteger notificationCount = new AtomicInteger();
        var sut =
                new GameBattingContext(
                        BatterTestData.swingOnlyLineUp(),
                        (_, _) -> notificationCount.incrementAndGet(),
                        factory);

        // when
        try (ScriptedRandom random =
                ScriptedRandom.repeating(Draws.STRIKEOUT, script.totalAtBats())) {
            while (!sut.isGameOver()) {
                sut.nextAtBat();
            }
            sut.nextAtBat();
            sut.completeInning();
            sut.nextAtBat();

            // then
            assertAll(
                    () -> assertTrue(sut.isGameOver()),
                    () -> assertEquals(9, sut.getInning(), "終了後もイニングが進まないこと"),
                    () -> assertEquals(1, sut.getTotalScore(), "終了後も得点が変わらないこと"),
                    () -> assertEquals(1, notificationCount.get(), "終了後に再通知しないこと"),
                    () -> assertEquals(27, random.consumedCount(), "終了後の打席は乱数を消費しないこと"),
                    () -> random.assertFullyConsumed(),
                    () -> factory.assertFullyPlayed());
        }
    }

    @Test
    @DisplayName("進行中イニングの得点も試合総得点に含まれる")
    void includesCurrentInningScoreInTotal() {
        // given
        var factory =
                ScriptedBaseStateFactory.of(
                        InningScript.ofRunsPerInning(2, 0, 0, 0, 0, 0, 0, 0, 0));
        var sut = new GameBattingContext(BatterTestData.swingOnlyLineUp(), (_, _) -> {}, factory);

        // when
        try (ScriptedRandom random = ScriptedRandom.of(Draws.STRIKEOUT)) {
            sut.nextAtBat();

            // then
            assertAll(
                    () -> assertEquals(1, sut.getInning(), "1回の途中であること"),
                    () -> assertFalse(sut.isGameOver(), "試合は続いていること"),
                    () -> assertEquals(2, sut.getTotalScore(), "終了済みイニングがなくても進行中の得点を含むこと"),
                    () -> random.assertFullyConsumed());
        }
    }

    @Test
    @DisplayName("イニングごとの打撃結果が試合統計へ積まれる")
    void accumulatesBattingResultsIntoGameStatistics() {
        // given
        var script = InningScript.ofRunsPerInning(1, 1, 1, 1, 1, 1, 1, 1, 1);
        var factory = ScriptedBaseStateFactory.of(script);
        AtomicReference<GameStatistics> notifiedStatistics = new AtomicReference<>();
        var sut =
                new GameBattingContext(
                        BatterTestData.swingOnlyLineUp(),
                        (_, gameStatistics) -> notifiedStatistics.set(gameStatistics),
                        factory);
        // 各イニングの先頭打席だけ本塁打にする。脚本の得点は本塁打とは独立に加算される。
        float[] perInning = {Draws.HOMER, Draws.STRIKEOUT, Draws.STRIKEOUT};
        float[] wholeGame = new float[0];
        for (int inning = 0; inning < InningScript.INNINGS_PER_GAME; inning++) {
            wholeGame = ScriptedRandom.concat(wholeGame, perInning);
        }

        // when
        try (ScriptedRandom random = ScriptedRandom.of(wholeGame)) {
            while (!sut.isGameOver()) {
                sut.nextAtBat();
            }

            // then
            GameStatistics statistics = sut.getGameStatistics();
            assertAll(
                    () -> assertEquals(9, sut.getTotalScore(), "得点は脚本どおりであること"),
                    () -> assertEquals(soloHomeRunStatistics(9), statistics, "本塁打9本が統計に積まれること"),
                    () -> assertEquals(statistics, notifiedStatistics.get(), "通知される統計が最終統計であること"),
                    () -> StatisticsAssertions.assertConsistent(statistics),
                    () -> assertEquals(27, random.consumedCount()),
                    () -> random.assertFullyConsumed(),
                    () -> factory.assertFullyPlayed());
        }
    }
}
