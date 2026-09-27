package com.example.baseballorders.simulator.domain.statistics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameTransitionRecorderTest {

    private static GameTransition transition(long inning, String actionResult) {
        return GameTransitionBuilder.gameTransition()
                .inning(inning)
                .actionResult(actionResult)
                .outCount(0)
                .cumulativeScore(0)
                .runnerState("走者なし")
                .build();
    }

    @Test
    @DisplayName("記録した状況推移を発生順に返す")
    void returnsRecordedTransitionsInOrder() {
        // given
        GameTransitionRecorder recorder = new GameTransitionRecorder();
        GameTransition first = transition(1, "単打");
        GameTransition second = transition(1, "三振");

        // when
        recorder.record(first);
        recorder.record(second);

        // then
        assertAll(() -> assertEquals(List.of(first, second), recorder.snapshot()));
    }

    @Test
    @DisplayName("何も記録していなければ空のリストを返す")
    void returnsEmptyListWhenNothingRecorded() {
        // given
        GameTransitionRecorder recorder = new GameTransitionRecorder();

        // when
        List<GameTransition> snapshot = recorder.snapshot();

        // then
        assertAll(() -> assertEquals(List.of(), snapshot));
    }

    @Test
    @DisplayName("取得したスナップショットは変更できない")
    void snapshotIsUnmodifiable() {
        // given
        GameTransitionRecorder recorder = new GameTransitionRecorder();
        recorder.record(transition(1, "単打"));
        List<GameTransition> snapshot = recorder.snapshot();

        // when
        UnsupportedOperationException exception =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> snapshot.add(transition(2, "三振")));

        // then
        assertAll(() -> assertEquals(1, snapshot.size(), "例外発生時もスナップショットの内容は変わらないこと"));
        // 例外自体の内容は実装依存のため検証しない
        assertAll(() -> assertEquals(UnsupportedOperationException.class, exception.getClass()));
    }

    @Test
    @DisplayName("スナップショット取得後に記録を追加してもスナップショットは変化しない")
    void snapshotIsNotAffectedByLaterRecords() {
        // given
        GameTransitionRecorder recorder = new GameTransitionRecorder();
        recorder.record(transition(1, "単打"));
        List<GameTransition> snapshot = recorder.snapshot();

        // when
        recorder.record(transition(1, "三振"));

        // then
        assertAll(() -> assertEquals(1, snapshot.size()));
    }
}
