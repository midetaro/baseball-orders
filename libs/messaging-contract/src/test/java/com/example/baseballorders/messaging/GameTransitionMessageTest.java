package com.example.baseballorders.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameTransitionMessageTest {

    @Test
    @DisplayName("StagedBuilderで1試合実行の推移を構築する")
    void buildsTransitionWithStagedBuilder() {
        // given

        // when
        var transition =
                GameTransitionMessageBuilder.gameTransitionMessage()
                        .inning(3)
                        .actionResult("4番 本塁打")
                        .outCount(1)
                        .cumulativeScore(2)
                        .runnerState("走者なし")
                        .build();

        // then
        assertAll(
                () -> assertEquals(3, transition.inning()),
                () -> assertEquals("4番 本塁打", transition.actionResult()),
                () -> assertEquals(1, transition.outCount()),
                () -> assertEquals(2, transition.cumulativeScore()),
                () -> assertEquals("走者なし", transition.runnerState()));
    }

    @Test
    @DisplayName("振る舞い結果がnullの推移を拒否する")
    void rejectsNullActionResult() {
        // given

        // when
        var exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new GameTransitionMessage(1, null, 0, 0, "走者なし"));

        // then
        assertAll(() -> assertEquals("actionResult must not be null", exception.getMessage()));
    }

    @Test
    @DisplayName("走者状況がnullの推移を拒否する")
    void rejectsNullRunnerState() {
        // given

        // when
        var exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new GameTransitionMessage(1, "1番 単打", 0, 1, null));

        // then
        assertAll(() -> assertEquals("runnerState must not be null", exception.getMessage()));
    }
}
