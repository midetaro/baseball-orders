package com.example.baseballorders.simulator.domain.statistics;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GameStatisticsRecorderTest {

    @Test
    @DisplayName("単打・二塁打・三塁打・本塁打を総安打数と内訳に記録する")
    void recordsAllHitTypes() {
        // given
        GameStatisticsRecorder recorder = new GameStatisticsRecorder();

        // when
        recorder.onBattingResult(BattingResult.HIT_SINGLE, 0);
        recorder.onBattingResult(BattingResult.HIT_DOUBLE, 0);
        recorder.onBattingResult(BattingResult.HIT_TRIPLE, 0);
        recorder.onBattingResult(BattingResult.HIT_HOMER, 0);
        recorder.onBattingResult(BattingResult.WALK, 0);
        recorder.onBattingResult(BattingResult.STRIKEOUT, 0);
        recorder.onBattingResult(BattingResult.BATTED_OUT, 0);
        GameStatistics statistics = recorder.snapshot();

        // then
        assertAll(
                () -> assertEquals(4, statistics.hitCount()),
                () -> assertEquals(1, statistics.singleHitCount()),
                () -> assertEquals(1, statistics.doubleHitCount()),
                () -> assertEquals(1, statistics.tripleHitCount()),
                () -> assertEquals(1, statistics.homeRunCount()));
    }

    static Stream<Arguments> homeRunTestCases() {
        return Stream.of(
                arguments("走者なしならソロ本塁打", 0, 1, 0, 0, 0),
                arguments("走者一人なら2点本塁打", 1, 0, 1, 0, 0),
                arguments("走者二人なら3点本塁打", 2, 0, 0, 1, 0),
                arguments("満塁なら満塁本塁打", 3, 0, 0, 0, 1));
    }

    @DisplayName("本塁打時の走者数に応じた種類を記録する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("homeRunTestCases")
    void recordsHomeRunType(
            String description,
            int runnerCount,
            int expectedSolo,
            int expectedTwoRun,
            int expectedThreeRun,
            int expectedGrandSlam) {
        // given
        GameStatisticsRecorder recorder = new GameStatisticsRecorder();

        // when
        recorder.onBattingResult(BattingResult.HIT_HOMER, runnerCount);

        // then
        GameStatistics statistics = recorder.snapshot();
        assertAll(
                description,
                () -> assertEquals(1, statistics.hitCount()),
                () -> assertEquals(1, statistics.homeRunCount()),
                () -> assertEquals(expectedSolo, statistics.soloHomeRunCount()),
                () -> assertEquals(expectedTwoRun, statistics.twoRunHomeRunCount()),
                () -> assertEquals(expectedThreeRun, statistics.threeRunHomeRunCount()),
                () -> assertEquals(expectedGrandSlam, statistics.grandSlamCount()));
    }

    @Test
    @DisplayName("不正な走者数では本塁打を記録しない")
    void rejectsInvalidRunnerCount() {
        // given
        GameStatisticsRecorder recorder = new GameStatisticsRecorder();

        // when
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> recorder.onBattingResult(BattingResult.HIT_HOMER, 4));

        // then
        assertAll(
                () -> assertEquals("runner count must be between 0 and 3", exception.getMessage()),
                () -> assertEquals(0, recorder.snapshot().hitCount()),
                () -> assertEquals(0, recorder.snapshot().homeRunCount()));
    }

    @Test
    @DisplayName("成功したバントと盗塁を記録する")
    void recordsBuntAndSteal() {
        // given
        GameStatisticsRecorder recorder = new GameStatisticsRecorder();

        // when
        recorder.onBuntResult(BuntResult.SUCCESS, BuntType.ADVANCING);
        recorder.onBuntResult(BuntResult.SUCCESS, BuntType.SQUEEZE);
        recorder.onStealResult(StealResult.SUCCESS, StealTarget.SECOND);
        recorder.onStealResult(StealResult.SUCCESS, StealTarget.THIRD);

        // then
        GameStatistics statistics = recorder.snapshot();
        assertAll(
                () -> assertEquals(2, statistics.buntCount()),
                () -> assertEquals(2, statistics.stealCount()),
                () -> assertEquals(1, statistics.advancingBuntCount()),
                () -> assertEquals(1, statistics.squeezeBuntCount()),
                () -> assertEquals(1, statistics.stealToSecondCount()),
                () -> assertEquals(1, statistics.stealToThirdCount()));
    }

    @Test
    @DisplayName("失敗したバントと盗塁を記録する")
    void recordsFailedBuntAndSteal() {
        // given
        GameStatisticsRecorder recorder = new GameStatisticsRecorder();

        // when
        recorder.onBuntResult(BuntResult.FAILURE, BuntType.ADVANCING);
        recorder.onBuntResult(BuntResult.FAILURE, BuntType.SQUEEZE);
        recorder.onStealResult(StealResult.FAILURE, StealTarget.SECOND);

        // then
        GameStatistics statistics = recorder.snapshot();
        assertAll(
                () -> assertEquals(2, statistics.buntFailureCount()),
                () -> assertEquals(1, statistics.stealFailureCount()),
                () -> assertEquals(1, statistics.advancingBuntFailureCount()),
                () -> assertEquals(1, statistics.squeezeBuntFailureCount()),
                () -> assertEquals(0, statistics.buntCount()),
                () -> assertEquals(0, statistics.stealCount()));
    }

    @Test
    @DisplayName("実行しなかったバントと盗塁は成功・失敗のいずれにも記録しない")
    void doesNotRecordUnattemptedBuntAndSteal() {
        // given
        GameStatisticsRecorder recorder = new GameStatisticsRecorder();

        // when
        recorder.onBuntResult(BuntResult.NOT_TRY, BuntType.ADVANCING);
        recorder.onStealResult(StealResult.NOT_TRY, StealTarget.SECOND);

        // then
        GameStatistics statistics = recorder.snapshot();
        assertAll(
                () -> assertEquals(0, statistics.buntCount()),
                () -> assertEquals(0, statistics.stealCount()),
                () -> assertEquals(0, statistics.buntFailureCount()),
                () -> assertEquals(0, statistics.stealFailureCount()),
                () -> assertEquals(0, statistics.advancingBuntCount()),
                () -> assertEquals(0, statistics.advancingBuntFailureCount()),
                () -> assertEquals(0, statistics.stealToSecondCount()));
    }
}
