package com.example.baseballorders.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationResultMessageTest {

    @Test
    @DisplayName("戦術の詳細集計をJSON既定プロパティ名として公開する")
    void exposesDetailedTacticalCountsAsWireProperties() {
        var statistics =
                new SimulationResultMessage.Statistics(
                        4.2,
                        4.0,
                        12,
                        100,
                        Map.of(4, 18),
                        25,
                        10,
                        8,
                        5,
                        2,
                        14,
                        11,
                        6,
                        3,
                        9,
                        5,
                        4,
                        2,
                        7,
                        4);

        var wirePropertyNames =
                Arrays.stream(SimulationResultMessage.Statistics.class.getRecordComponents())
                        .map(component -> component.getName())
                        .toList();

        assertAll(
                () ->
                        assertEquals(
                                List.of(
                                        "averageScore",
                                        "medianScore",
                                        "maximumScore",
                                        "gameCount",
                                        "scoreDistribution",
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
                                wirePropertyNames),
                () -> assertEquals(9, statistics.advancingBuntCount()),
                () -> assertEquals(5, statistics.squeezeBuntCount()),
                () -> assertEquals(4, statistics.advancingBuntFailureCount()),
                () -> assertEquals(2, statistics.squeezeBuntFailureCount()),
                () -> assertEquals(7, statistics.stealToSecondCount()),
                () -> assertEquals(4, statistics.stealToThirdCount()));
    }

    @Test
    @DisplayName("旧形式の統計から復元した場合は戦術の詳細集計をゼロにする")
    void defaultsDetailedTacticalCountsForLegacyStatistics() {
        var statistics =
                new SimulationResultMessage.Statistics(
                        4.2, 4.0, 12, 100, Map.of(4, 18), 25, 10, 8, 5, 2, 14, 11, 6, 3);

        assertAll(
                () -> assertEquals(14, statistics.buntCount()),
                () -> assertEquals(11, statistics.stealCount()),
                () -> assertEquals(6, statistics.buntFailureCount()),
                () -> assertEquals(3, statistics.stealFailureCount()),
                () -> assertEquals(0, statistics.advancingBuntCount()),
                () -> assertEquals(0, statistics.squeezeBuntCount()),
                () -> assertEquals(0, statistics.advancingBuntFailureCount()),
                () -> assertEquals(0, statistics.squeezeBuntFailureCount()),
                () -> assertEquals(0, statistics.stealToSecondCount()),
                () -> assertEquals(0, statistics.stealToThirdCount()));
    }
}
