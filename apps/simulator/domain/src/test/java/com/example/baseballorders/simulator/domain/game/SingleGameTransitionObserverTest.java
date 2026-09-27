package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.play.StealTarget;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.player.BatterTestData;
import com.example.baseballorders.simulator.domain.statistics.GameTransition;
import com.example.baseballorders.simulator.domain.statistics.GameTransitionRecorder;
import com.example.baseballorders.simulator.domain.statistics.PlayResultObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SingleGameTransitionObserverTest {

    /** プレー結果の転送先を検証するための記録用フェイク。 */
    private static final class RecordingDelegate implements PlayResultObserver {
        private final List<BattingResult> battingResults = new ArrayList<>();
        private final List<BuntResult> buntResults = new ArrayList<>();
        private final List<StealResult> stealResults = new ArrayList<>();

        @Override
        public void onBattingResult(BattingResult battingResult, int runnerCount) {
            battingResults.add(battingResult);
        }

        @Override
        public void onBuntResult(BuntResult buntResult, BuntType buntType) {
            buntResults.add(buntResult);
        }

        @Override
        public void onStealResult(StealResult stealResult, StealTarget stealTarget) {
            stealResults.add(stealResult);
        }
    }

    private static SingleGameTransitionObserver observerFor(
            GameBattingContext context,
            PlayResultObserver delegate,
            GameTransitionRecorder recorder) {
        return new SingleGameTransitionObserver(
                delegate,
                recorder,
                context.inningStateContext(),
                context::getInning,
                context::getTotalScore);
    }

    @Test
    @DisplayName("打撃結果を委譲先へ転送しつつ、初期状態の状況推移として記録する")
    void recordsBattingResultWithInitialState() {
        // given
        BatterEntity batter = BatterTestData.swingOnlyBatter();
        GameBattingContext context =
                GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);
        RecordingDelegate delegate = new RecordingDelegate();
        GameTransitionRecorder recorder = new GameTransitionRecorder();
        SingleGameTransitionObserver sut = observerFor(context, delegate, recorder);

        // when
        sut.onBattingResult(BattingResult.HIT_SINGLE, 0);

        // then
        List<GameTransition> transitions = recorder.snapshot();
        assertAll(
                () -> assertEquals(List.of(BattingResult.HIT_SINGLE), delegate.battingResults),
                () -> assertEquals(1, transitions.size()),
                () -> assertEquals(1, transitions.get(0).inning()),
                () -> assertEquals("単打", transitions.get(0).actionResult()),
                () -> assertEquals(0, transitions.get(0).outCount()),
                () -> assertEquals(0, transitions.get(0).cumulativeScore()),
                () -> assertEquals("走者なし", transitions.get(0).runnerState()));
    }

    @Test
    @DisplayName("一塁・三塁に走者、一死の状況で盗塁失敗を転送しつつ、その時点の状況として記録する")
    void recordsStealResultWithCurrentState() {
        // given
        BatterEntity runner = BatterTestData.swingOnlyBatter();
        GameBattingContext context =
                GameStateTestFixture.context(runner, null, runner, OutCount.ONE_OUT);
        RecordingDelegate delegate = new RecordingDelegate();
        GameTransitionRecorder recorder = new GameTransitionRecorder();
        SingleGameTransitionObserver sut = observerFor(context, delegate, recorder);

        // when
        sut.onStealResult(StealResult.FAILURE, StealTarget.SECOND);

        // then
        List<GameTransition> transitions = recorder.snapshot();
        assertAll(
                () -> assertEquals(List.of(StealResult.FAILURE), delegate.stealResults),
                () -> assertEquals(1, transitions.size()),
                () -> assertEquals("盗塁失敗(二塁)", transitions.get(0).actionResult()),
                () -> assertEquals(1, transitions.get(0).outCount()),
                () -> assertEquals("一・三塁", transitions.get(0).runnerState()));
    }

    static Stream<Arguments> unattemptedPlayTestCases() {
        return Stream.of(
                arguments(
                        "未実行のバントは推移として記録しない",
                        (java.util.function.Consumer<SingleGameTransitionObserver>)
                                observer ->
                                        observer.onBuntResult(
                                                BuntResult.NOT_TRY, BuntType.ADVANCING)),
                arguments(
                        "未実行の盗塁は推移として記録しない",
                        (java.util.function.Consumer<SingleGameTransitionObserver>)
                                observer ->
                                        observer.onStealResult(
                                                StealResult.NOT_TRY, StealTarget.SECOND)));
    }

    @DisplayName("未実行のバント・盗塁は委譲先へ転送しつつ、状況推移としては記録しない")
    @ParameterizedTest(name = "{0}")
    @MethodSource("unattemptedPlayTestCases")
    void doesNotRecordUnattemptedPlays(
            String description, java.util.function.Consumer<SingleGameTransitionObserver> action) {
        // given
        GameBattingContext context =
                GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);
        RecordingDelegate delegate = new RecordingDelegate();
        GameTransitionRecorder recorder = new GameTransitionRecorder();
        SingleGameTransitionObserver sut = observerFor(context, delegate, recorder);

        // when
        action.accept(sut);

        // then
        assertAll(description, () -> assertEquals(0, recorder.snapshot().size()));
    }

    static Stream<Arguments> buntDescriptionTestCases() {
        return Stream.of(
                arguments(BuntResult.SUCCESS, BuntType.ADVANCING, "バント成功"),
                arguments(BuntResult.FAILURE, BuntType.ADVANCING, "バント失敗"),
                arguments(BuntResult.SUCCESS, BuntType.SQUEEZE, "スクイズ成功"),
                arguments(BuntResult.FAILURE, BuntType.SQUEEZE, "スクイズ失敗"));
    }

    @DisplayName("バント結果と種別の組み合わせごとに日本語の説明を記録する")
    @ParameterizedTest(name = "{0}/{1} -> {2}")
    @MethodSource("buntDescriptionTestCases")
    void describesBuntResult(BuntResult buntResult, BuntType buntType, String expected) {
        // given
        GameBattingContext context =
                GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);
        GameTransitionRecorder recorder = new GameTransitionRecorder();
        SingleGameTransitionObserver sut = observerFor(context, new RecordingDelegate(), recorder);

        // when
        sut.onBuntResult(buntResult, buntType);

        // then
        assertAll(() -> assertEquals(expected, recorder.snapshot().get(0).actionResult()));
    }

    static Stream<Arguments> stealDescriptionTestCases() {
        return Stream.of(
                arguments(StealResult.SUCCESS, StealTarget.SECOND, "盗塁成功(二塁)"),
                arguments(StealResult.FAILURE, StealTarget.SECOND, "盗塁失敗(二塁)"),
                arguments(StealResult.SUCCESS, StealTarget.THIRD, "盗塁成功(三塁)"),
                arguments(StealResult.FAILURE, StealTarget.THIRD, "盗塁失敗(三塁)"));
    }

    @DisplayName("盗塁結果と目標塁の組み合わせごとに日本語の説明を記録する")
    @ParameterizedTest(name = "{0}/{1} -> {2}")
    @MethodSource("stealDescriptionTestCases")
    void describesStealResult(StealResult stealResult, StealTarget stealTarget, String expected) {
        // given
        GameBattingContext context =
                GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);
        GameTransitionRecorder recorder = new GameTransitionRecorder();
        SingleGameTransitionObserver sut = observerFor(context, new RecordingDelegate(), recorder);

        // when
        sut.onStealResult(stealResult, stealTarget);

        // then
        assertAll(() -> assertEquals(expected, recorder.snapshot().get(0).actionResult()));
    }

    static Stream<Arguments> runnerStateTestCases() {
        BatterEntity runner = BatterTestData.swingOnlyBatter();
        return Stream.of(
                arguments("走者なし", null, null, null),
                arguments("一塁", runner, null, null),
                arguments("二塁", null, runner, null),
                arguments("一・二塁", runner, runner, null),
                arguments("三塁", null, null, runner),
                arguments("一・三塁", runner, null, runner),
                arguments("二・三塁", null, runner, runner),
                arguments("満塁", runner, runner, runner));
    }

    @DisplayName("走者配置ごとに日本語の走者状況を記録する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("runnerStateTestCases")
    void describesRunnerState(
            String expected, BatterEntity first, BatterEntity second, BatterEntity third) {
        // given
        GameBattingContext context =
                GameStateTestFixture.context(first, second, third, OutCount.NO_OUT);
        GameTransitionRecorder recorder = new GameTransitionRecorder();
        SingleGameTransitionObserver sut = observerFor(context, new RecordingDelegate(), recorder);

        // when
        sut.onBattingResult(BattingResult.STRIKEOUT, 0);

        // then
        assertAll(() -> assertEquals(expected, recorder.snapshot().get(0).runnerState()));
    }
}
