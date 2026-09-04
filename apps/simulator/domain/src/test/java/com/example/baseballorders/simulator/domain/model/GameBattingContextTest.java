package com.example.baseballorders.simulator.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.model.state.NoBasesState;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
@Getter
public class GameBattingContextTest {

    @DisplayName("addOutCounts() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("addOutCountsTestCases")
    public void addOutCountsTest(
            String description,
            int initialInning,
            int addOutCount,
            OutCount expectedOutCount,
            int expectedInning,
            boolean expectedGameOver) {
        // given
        GameBattingContext gameBattingContext =
                new GameBattingContext(new LineUpEntity(BatterTestDataFactory.mock()));
        if (initialInning > 1) {
            // 設定するためにリフレクションまたは複数回の呼び出しで調整
            for (int i = 1; i < initialInning; i++) {
                gameBattingContext.addOutCounts(3);
            }
        }

        // when
        gameBattingContext.addOutCounts(addOutCount);

        // then
        assertAll(
                () -> assertEquals(expectedOutCount, gameBattingContext.getOutCount(), description),
                () -> assertEquals(expectedInning, gameBattingContext.getInning(), description),
                () -> assertEquals(expectedGameOver, gameBattingContext.isGameOver(), description));
    }

    static Stream<Arguments> addOutCountsTestCases() {
        return Stream.of(
                // 分岐1: outCounts < 3（goToNextInning呼ばれない）
                arguments(
                        "[分岐1-1] addOutCounts(1) → outCounts=1（goToNextInning スキップ）",
                        1,
                        1,
                        OutCount.ONE_OUT,
                        1,
                        false),
                arguments(
                        "[分岐1-2] addOutCounts(2) → outCounts=2（goToNextInning スキップ）",
                        1,
                        2,
                        OutCount.TWO_OUT,
                        1,
                        false),
                // 分岐2a: outCounts >= 3 かつ inning != 9（イニング進行）
                arguments(
                        "[分岐2a] addOutCounts(3)、inning=1 → inning=2, outCounts=0（リセット）",
                        1,
                        3,
                        OutCount.NO_OUT,
                        2,
                        false),
                arguments(
                        "[分岐2a] addOutCounts(3)、inning=2 → inning=3, outCounts=0（リセット）",
                        2,
                        3,
                        OutCount.NO_OUT,
                        3,
                        false),
                // 分岐2b: outCounts >= 3 かつ inning == 9（ゲーム終了）
                arguments(
                        "[分岐2b] addOutCounts(3)、inning=9 → isGameOver=true, outCounts=0",
                        9,
                        3,
                        OutCount.NO_OUT,
                        9,
                        true));
    }

    @DisplayName("addScore() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("addScoreTestCases")
    public void addScoreTest(
            String description, long initialScore, long addScore, long expectedScore) {
        // given
        GameBattingContext gameBattingContext =
                new GameBattingContext(new LineUpEntity(BatterTestDataFactory.mock()));
        // 初期スコア設定
        for (int i = 0; i < initialScore; i++) {
            gameBattingContext.addScore(1);
        }

        // when
        gameBattingContext.addScore(addScore);

        // then
        assertAll(
                () -> assertEquals(expectedScore, gameBattingContext.getTotalScore(), description));
    }

    static Stream<Arguments> addScoreTestCases() {
        return Stream.of(
                // スコアが正常に加算されるケース
                arguments("[基本] addScore(0) → totalScore=0", 0, 0, 0),
                arguments("[基本] addScore(1) → totalScore=1", 0, 1, 1),
                arguments("[基本] addScore(3) → totalScore=3", 0, 3, 3),
                arguments("[累積] initialScore=5 + addScore(2) → totalScore=7", 5, 2, 7));
    }

    @DisplayName("updateBaseState() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("updateBaseStateTestCases")
    public void updateBaseStateTest(
            String description,
            com.example.baseballorders.simulator.domain.model.state.BasesState newState) {
        // given
        GameBattingContext gameBattingContext =
                new GameBattingContext(new LineUpEntity(BatterTestDataFactory.mock()));

        // when
        gameBattingContext.updateBaseState(newState);

        // then
        assertAll(
                () ->
                        assertEquals(
                                newState, gameBattingContext.getCurrentBaseState(), description));
    }

    static Stream<Arguments> updateBaseStateTestCases() {
        return Stream.of(
                arguments("[初期状態→NoBasesState] 塁が空になる", new NoBasesState()),
                arguments("[初期状態→SingleBasesState] 1塁に走者がいる状態", new SingleBasesState()));
    }

    @DisplayName("setRunnerTo() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("setRunnerToTestCases")
    public void setRunnerToTest(
            String description, Base base, boolean hasBatter, int expectedBaseCount) {
        // given
        GameBattingContext gameBattingContext =
                new GameBattingContext(new LineUpEntity(BatterTestDataFactory.mock()));
        Optional<BatterEntity> batter =
                hasBatter ? Optional.of(BatterTestDataFactory.mock().get(0)) : Optional.empty();

        // when
        gameBattingContext.setRunnerTo(base, batter);

        // then
        Optional<BatterEntity> runner =
                switch (base) {
                    case FIRST -> gameBattingContext.getRunnerOnFirstBase();
                    case SECOND -> gameBattingContext.getRunnerOnSecondBase();
                    case THIRD -> gameBattingContext.getRunnerOnThirdBase();
                };
        assertAll(() -> assertEquals(hasBatter, runner.isPresent(), description));
    }

    static Stream<Arguments> setRunnerToTestCases() {
        return Stream.of(
                arguments("[1塁] 走者を配置", Base.FIRST, true, 1),
                arguments("[1塁] 走者を削除", Base.FIRST, false, 0),
                arguments("[2塁] 走者を配置", Base.SECOND, true, 2),
                arguments("[2塁] 走者を削除", Base.SECOND, false, 0),
                arguments("[3塁] 走者を配置", Base.THIRD, true, 3),
                arguments("[3塁] 走者を削除", Base.THIRD, false, 0));
    }

    @DisplayName("moveRunnerNthBase() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("moveRunnerNthBaseTestCases")
    public void moveRunnerNthBaseTest(
            String description,
            Base targetBase,
            boolean expectedFirstEmpty,
            boolean expectedSecondEmpty,
            boolean expectedThirdEmpty) {
        // given
        GameBattingContext gameBattingContext =
                new GameBattingContext(new LineUpEntity(BatterTestDataFactory.mock()));
        var batter = BatterTestDataFactory.mock().get(0);
        gameBattingContext.setRunnerTo(Base.FIRST, Optional.of(batter));
        gameBattingContext.setRunnerTo(Base.SECOND, Optional.of(batter));
        gameBattingContext.setRunnerTo(Base.THIRD, Optional.of(batter));

        // when
        gameBattingContext.moveRunnerNthBase(targetBase);

        // then
        assertAll(
                () ->
                        assertEquals(
                                expectedFirstEmpty,
                                gameBattingContext.getRunnerOnFirstBase().isEmpty(),
                                description),
                () ->
                        assertEquals(
                                expectedSecondEmpty,
                                gameBattingContext.getRunnerOnSecondBase().isEmpty(),
                                description),
                () ->
                        assertEquals(
                                expectedThirdEmpty,
                                gameBattingContext.getRunnerOnThirdBase().isEmpty(),
                                description));
    }

    static Stream<Arguments> moveRunnerNthBaseTestCases() {
        return Stream.of(
                // targetBase=1: 1塁へ（1-2-3塁→空-1-2）
                arguments("[targetBase=1] 1塁へ移動: 全塁走者→1,2塁に配置", Base.FIRST, true, false, false),
                // targetBase=2: 2塁へ（1-2-3塁→空-空-2）
                arguments("[targetBase=2] 2塁へ移動: 全塁走者→2塁のみ", Base.SECOND, true, true, false),
                // targetBase=3: 3塁へ（1-2-3塁→空-空-空）
                arguments("[targetBase=3] 3塁へ移動: 全塁走者→全て得点", Base.THIRD, true, true, true));
    }

    @DisplayName("isGameOver() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("isGameOverTestCases")
    public void isGameOverTest(String description, int addOutCountTimes, boolean expectedGameOver) {
        // given
        GameBattingContext gameBattingContext =
                new GameBattingContext(new LineUpEntity(BatterTestDataFactory.mock()));

        // when - 9回のアウトカウント満了でゲーム終了
        for (int i = 1; i <= addOutCountTimes; i++) {
            gameBattingContext.addOutCounts(3);
        }

        // then
        assertAll(
                () -> assertEquals(expectedGameOver, gameBattingContext.isGameOver(), description));
    }

    static Stream<Arguments> isGameOverTestCases() {
        return Stream.of(
                arguments("[ゲーム中] 1回アウト満了 → isGameOver=false", 1, false),
                arguments("[ゲーム中] 8回アウト満了 → isGameOver=false", 8, false),
                arguments("[ゲーム終了] 9回アウト満了 → isGameOver=true", 9, true));
    }

    @DisplayName("updateBaseStateOf() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("updateBaseStateOfTestCases")
    public void updateBaseStateOfTest(
            String description,
            boolean hasFirst,
            boolean hasSecond,
            boolean hasThird,
            Class<?> expectedStateClass) {
        // given
        GameBattingContext gameBattingContext =
                new GameBattingContext(new LineUpEntity(BatterTestDataFactory.mock()));
        var batter = BatterTestDataFactory.mock().get(0);

        // 各塁に走者を設定
        if (hasFirst) gameBattingContext.setRunnerTo(Base.FIRST, Optional.of(batter));
        if (hasSecond) gameBattingContext.setRunnerTo(Base.SECOND, Optional.of(batter));
        if (hasThird) gameBattingContext.setRunnerTo(Base.THIRD, Optional.of(batter));

        // when
        gameBattingContext.updateBaseStateOf();

        // then
        assertAll(
                () ->
                        assertTrue(
                                expectedStateClass.isInstance(
                                        gameBattingContext.getCurrentBaseState()),
                                description));
    }

    static Stream<Arguments> updateBaseStateOfTestCases() {
        return Stream.of(
                arguments(
                        "[分岐1] 満塁 → FullBasesState",
                        true,
                        true,
                        true,
                        com.example.baseballorders.simulator.domain.model.state.FullBasesState
                                .class),
                arguments(
                        "[分岐2] 1,2塁 → FirstDoubleBaseState",
                        true,
                        true,
                        false,
                        com.example.baseballorders.simulator.domain.model.state.FirstDoubleBaseState
                                .class),
                arguments(
                        "[分岐3] 1,3塁 → FirstThirdBaseState",
                        true,
                        false,
                        true,
                        com.example.baseballorders.simulator.domain.model.state.FirstThirdBaseState
                                .class),
                arguments(
                        "[分岐4] 1塁のみ → SingleBasesState",
                        true,
                        false,
                        false,
                        SingleBasesState.class),
                arguments(
                        "[分岐5] 2,3塁 → DoubleThirdBaseState",
                        false,
                        true,
                        true,
                        com.example.baseballorders.simulator.domain.model.state.DoubleThirdBaseState
                                .class),
                arguments(
                        "[分岐6] 2塁のみ → DoubleBaseState",
                        false,
                        true,
                        false,
                        com.example.baseballorders.simulator.domain.model.state.DoubleBaseState
                                .class),
                arguments(
                        "[分岐7] 3塁のみ → ThirdBaseState",
                        false,
                        false,
                        true,
                        com.example.baseballorders.simulator.domain.model.state.ThirdBaseState
                                .class),
                arguments("[分岐8] 塁なし → NoBasesState", false, false, false, NoBasesState.class));
    }

    @DisplayName("nextAtBat() - 打者交代テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("nextAtBatTestCases")
    public void nextAtBatTest(
            String description, int numberOfNextBatterBefore, int expectedNumberOfNextBatterAfter) {
        // given
        GameBattingContext gameBattingContext =
                new GameBattingContext(new LineUpEntity(BatterTestDataFactory.mock()));
        // numberOfNextBatter を設定するためにループで実行
        for (int i = 0; i < numberOfNextBatterBefore; i++) {
            gameBattingContext.nextAtBat();
        }

        // when
        gameBattingContext.nextAtBat();

        // then
        assertAll(
                () ->
                        assertEquals(
                                expectedNumberOfNextBatterAfter,
                                gameBattingContext.getNumberOfNextBatter(),
                                description));
    }

    static Stream<Arguments> nextAtBatTestCases() {
        return Stream.of(
                // 通常の打者交代（0～7番目）
                arguments("[打者交代] 0番目 → 1番目", 0, 1),
                arguments("[打者交代] 1番目 → 2番目", 1, 2),
                arguments("[打者交代] 2番目 → 3番目", 2, 3),
                arguments("[打者交代] 3番目 → 4番目", 3, 4),
                arguments("[打者交代] 4番目 → 5番目", 4, 5),
                arguments("[打者交代] 5番目 → 6番目", 5, 6),
                arguments("[打者交代] 6番目 → 7番目", 6, 7),
                arguments("[打者交代] 7番目 → 8番目", 7, 8),
                // 順番が一周して最初に戻る（8番目 → 0 → 1）
                arguments("[打者交代・周回] 8番目 → 1番目", 8, 1));
    }

    @DisplayName("打席結果に応じた進塁処理を実行する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("battingResultTestCases")
    void appliesEveryBattingResult(
            String description,
            BattingResult battingResult,
            Class<?> expectedState,
            long expectedScore,
            OutCount expectedOutCount) {
        // given
        BatterEntity batter = batter(battingResult, StealResult.NOT_TRY);
        GameBattingContext context =
                new GameBattingContext(new LineUpEntity(Collections.nCopies(9, batter)));

        // when
        context.nextAtBat();

        // then
        assertAll(
                () ->
                        assertTrue(
                                expectedState.isInstance(context.getCurrentBaseState()),
                                description),
                () -> assertEquals(expectedScore, context.getTotalScore(), description),
                () -> assertEquals(expectedOutCount, context.getOutCount(), description));
    }

    static Stream<Arguments> battingResultTestCases() {
        return Stream.of(
                arguments(
                        "アウトならアウトカウントが増える",
                        BattingResult.OUT,
                        NoBasesState.class,
                        0,
                        OutCount.ONE_OUT),
                arguments(
                        "単打なら打者が一塁へ進む",
                        BattingResult.HIT_SINGLE,
                        SingleBasesState.class,
                        0,
                        OutCount.NO_OUT),
                arguments(
                        "二塁打なら打者が二塁へ進む",
                        BattingResult.HIT_DOUBLE,
                        com.example.baseballorders.simulator.domain.model.state.DoubleBaseState
                                .class,
                        0,
                        OutCount.NO_OUT),
                arguments(
                        "三塁打なら打者が三塁へ進む",
                        BattingResult.HIT_TRIPLE,
                        com.example.baseballorders.simulator.domain.model.state.ThirdBaseState
                                .class,
                        0,
                        OutCount.NO_OUT),
                arguments(
                        "本塁打なら一点入る",
                        BattingResult.HIT_HOMER,
                        NoBasesState.class,
                        1,
                        OutCount.NO_OUT));
    }

    @DisplayName("一塁走者が盗塁に失敗すると走者が消えてアウトが増える")
    @org.junit.jupiter.api.Test
    void appliesFailedStealResult() {
        // given
        BatterEntity runner = batter(BattingResult.OUT, StealResult.FAILURE);
        BatterEntity hitter = batter(BattingResult.HIT_SINGLE, StealResult.NOT_TRY);
        GameBattingContext context =
                new GameBattingContext(new LineUpEntity(Collections.nCopies(9, hitter)));
        context.setRunnerOnFirstBase(Optional.of(runner));
        context.updateBaseStateOf();

        // when
        context.nextAtBat();

        // then
        assertAll(
                () -> assertEquals(OutCount.ONE_OUT, context.getOutCount()),
                () -> assertEquals(Optional.of(hitter), context.getRunnerOnFirstBase()));
    }

    private static BatterEntity batter(BattingResult battingResult, StealResult stealResult) {
        return new BatterEntity(
                "batter",
                0.0f,
                0.0f,
                0.0f,
                (hitAverage, sluggish) -> battingResult,
                new FixedStealStrategy(stealResult),
                (successRate, outCount, basesState) -> BuntResult.NOT_TRY);
    }

    private record FixedStealStrategy(StealResult result) implements StealStrategy {

        @Override
        public StealResult runToDouble() {
            return result;
        }

        @Override
        public StealResult runToTriple() {
            return result;
        }
    }
}
