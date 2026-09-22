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
                        25, 10, 8, 5, 2, 14, 11, 6, 3, 9, 5, 4, 2, 7, 4);
        var sut =
                new SimulationResultMessage(
                        UUID.randomUUID(), SimulationResultMessage.CURRENT_VERSION, score, content);

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
                                        "gameContentStatistics"),
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
                () -> assertEquals("2", sut.version()),
                () -> assertEquals(9, sut.gameContentStatistics().advancingBuntCount()));
    }
}
