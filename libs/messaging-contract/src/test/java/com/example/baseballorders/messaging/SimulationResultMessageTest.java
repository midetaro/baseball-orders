package com.example.baseballorders.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationResultMessageTest {

    @Test
    @DisplayName("得点統計と試合内容統計を別々のJSONプロパティとして公開する")
    void separatesScoreAndContentStatisticsOnWire() {
        // given
        var score =
                new SimulationResultMessage.GameScoreStatistics(4.2, 4.0, 12, 100, Map.of(4, 18));
        var content =
                new SimulationResultMessage.GameContentStatistics(
                        40, 15, 10, 8, 25, 10, 8, 5, 2, 14, 11, 6, 3, 9, 5, 4, 2, 7, 4);
        var sut =
                SimulationResultMessageBuilder.simulationResultMessage()
                        .simulationId(UUID.randomUUID())
                        .version(SimulationResultMessage.CURRENT_VERSION)
                        .gameScoreStatistics(score)
                        .gameContentStatistics(content)
                        .gameTransitions(List.of())
                        .build();

        // when
        var messageProperties =
                Arrays.stream(SimulationResultMessage.class.getRecordComponents())
                        .map(component -> component.getName())
                        .toList();
        var scoreProperties =
                Arrays.stream(
                                SimulationResultMessage.GameScoreStatistics.class
                                        .getRecordComponents())
                        .map(component -> component.getName())
                        .toList();
        var contentProperties =
                Arrays.stream(
                                SimulationResultMessage.GameContentStatistics.class
                                        .getRecordComponents())
                        .map(component -> component.getName())
                        .toList();

        // then
        assertAll(
                () ->
                        assertEquals(
                                List.of(
                                        "simulationId",
                                        "version",
                                        "gameScoreStatistics",
                                        "gameContentStatistics",
                                        "gameTransitions"),
                                messageProperties),
                () ->
                        assertEquals(
                                List.of(
                                        "averageScore",
                                        "medianScore",
                                        "maximumScore",
                                        "gameCount",
                                        "scoreDistribution"),
                                scoreProperties),
                () ->
                        assertEquals(
                                List.of(
                                        "hitCount",
                                        "singleHitCount",
                                        "doubleHitCount",
                                        "tripleHitCount",
                                        "homeRunCount",
                                        "soloHomeRunCount",
                                        "twoRunHomeRunCount",
                                        "threeRunHomeRunCount",
                                        "grandSlamCount",
                                        "buntCount",
                                        "stealCount",
                                        "buntFailureCount",
                                        "stealFailureCount",
                                        "advancingBuntCount",
                                        "squeezeBuntCount",
                                        "advancingBuntFailureCount",
                                        "squeezeBuntFailureCount",
                                        "stealToSecondCount",
                                        "stealToThirdCount"),
                                contentProperties),
                () -> assertEquals(100, sut.gameScoreStatistics().gameCount()),
                () -> assertEquals("4", sut.version()),
                () -> assertEquals(40, sut.gameContentStatistics().hitCount()),
                () -> assertEquals(9, sut.gameContentStatistics().advancingBuntCount()),
                () -> assertEquals(List.of(), sut.gameTransitions()));
    }

    @Test
    @DisplayName("1試合実行の推移リストをSQS結果メッセージに含める")
    void includesGameTransitionsForSingleGameRun() {
        // given
        var transition =
                GameTransitionMessageBuilder.gameTransitionMessage()
                        .inning(1)
                        .actionResult("1番 単打")
                        .outCount(0)
                        .cumulativeScore(1)
                        .runnerState("一塁")
                        .build();

        // when
        var sut =
                SimulationResultMessageBuilder.simulationResultMessage()
                        .simulationId(UUID.randomUUID())
                        .version(SimulationResultMessage.CURRENT_VERSION)
                        .gameScoreStatistics(null)
                        .gameContentStatistics(null)
                        .gameTransitions(List.of(transition))
                        .build();

        // then
        assertAll(
                () -> assertEquals(1, sut.gameTransitions().size()),
                () -> assertEquals("1番 単打", sut.gameTransitions().get(0).actionResult()),
                () -> assertEquals("一塁", sut.gameTransitions().get(0).runnerState()));
    }

    @Test
    @DisplayName("廃止された移行用コンストラクタは推移リストを空で既定値にする")
    void defaultsGameTransitionsToEmptyForLegacyConstructor() {
        // given
        var simulationId = UUID.randomUUID();

        // when
        var sut =
                new SimulationResultMessage(
                        simulationId, SimulationResultMessage.CURRENT_VERSION, List.of());

        // then
        assertAll(() -> assertEquals(List.of(), sut.gameTransitions()));
    }
}
