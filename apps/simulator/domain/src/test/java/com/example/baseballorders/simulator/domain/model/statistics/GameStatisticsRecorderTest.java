package com.example.baseballorders.simulator.domain.model.statistics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GameStatisticsRecorderTest {

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
                () -> assertEquals(1, statistics.homeRunCount()),
                () -> assertEquals(expectedSolo, statistics.soloHomeRunCount()),
                () -> assertEquals(expectedTwoRun, statistics.twoRunHomeRunCount()),
                () -> assertEquals(expectedThreeRun, statistics.threeRunHomeRunCount()),
                () -> assertEquals(expectedGrandSlam, statistics.grandSlamCount()));
    }

    static Stream<Arguments> homeRunTestCases() {
        return Stream.of(
                arguments("走者なしならソロ本塁打", 0, 1, 0, 0, 0),
                arguments("走者一人なら2点本塁打", 1, 0, 1, 0, 0),
                arguments("走者二人なら3点本塁打", 2, 0, 0, 1, 0),
                arguments("満塁なら満塁本塁打", 3, 0, 0, 0, 1));
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
                () -> assertEquals(0, recorder.snapshot().homeRunCount()));
    }

    @Test
    @DisplayName("成功したバントと盗塁を記録する")
    void recordsBuntAndSteal() {
        // given
        GameStatisticsRecorder recorder = new GameStatisticsRecorder();

        // when
        recorder.onBuntResult(BuntResult.SUCCESS);
        recorder.onStealResult(StealResult.SUCCESS);

        // then
        GameStatistics statistics = recorder.snapshot();
        assertAll(
                () -> assertEquals(1, statistics.buntCount()),
                () -> assertEquals(1, statistics.stealCount()));
    }

    @Test
    @DisplayName("失敗したバントと盗塁を記録する")
    void recordsFailedBuntAndSteal() {
        // given
        GameStatisticsRecorder recorder = new GameStatisticsRecorder();

        // when
        recorder.onBuntResult(BuntResult.FAILURE);
        recorder.onStealResult(StealResult.FAILURE);

        // then
        GameStatistics statistics = recorder.snapshot();
        assertAll(
                () -> assertEquals(1, statistics.buntFailureCount()),
                () -> assertEquals(1, statistics.stealFailureCount()),
                () -> assertEquals(0, statistics.buntCount()),
                () -> assertEquals(0, statistics.stealCount()));
    }

    @Test
    @DisplayName("実行しなかったバントと盗塁は成功・失敗のいずれにも記録しない")
    void doesNotRecordUnattemptedBuntAndSteal() {
        // given
        GameStatisticsRecorder recorder = new GameStatisticsRecorder();

        // when
        recorder.onBuntResult(BuntResult.NOT_TRY);
        recorder.onStealResult(StealResult.NOT_TRY);

        // then
        GameStatistics statistics = recorder.snapshot();
        assertAll(
                () -> assertEquals(0, statistics.buntCount()),
                () -> assertEquals(0, statistics.stealCount()),
                () -> assertEquals(0, statistics.buntFailureCount()),
                () -> assertEquals(0, statistics.stealFailureCount()));
    }
}
