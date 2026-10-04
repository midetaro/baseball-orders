package com.example.baseballorders.backend.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultBatterTest {

    private static DefaultBatter batter(int order, float average, PlayerPersonality personality) {
        return DefaultBatterBuilder.defaultBatter()
                .battingOrder(order)
                .hitAverage(average)
                .personality(personality)
                .stealForced(false)
                .buntForced(false)
                .build();
    }

    @Test
    @DisplayName("有効な値から既定打者を生成できる")
    void createsBatterFromValidValues() {
        // given
        // when
        DefaultBatter batter = batter(1, 0.3f, PlayerPersonality.EAGER_STEAL);

        // then
        assertAll(
                () -> assertEquals(1, batter.battingOrder()),
                () -> assertEquals(0.3f, batter.hitAverage()),
                () -> assertEquals(PlayerPersonality.EAGER_STEAL, batter.personality()));
    }

    @Test
    @DisplayName("打順が1から9の範囲外なら拒否する")
    void rejectsBattingOrderOutOfRange() {
        // given
        // when
        IllegalArgumentException low =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> batter(0, 0.3f, PlayerPersonality.DEFAULT));
        IllegalArgumentException high =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> batter(10, 0.3f, PlayerPersonality.DEFAULT));

        // then
        assertAll(
                () -> assertEquals("battingOrder must be between 1 and 9", low.getMessage()),
                () -> assertEquals("battingOrder must be between 1 and 9", high.getMessage()));
    }

    @Test
    @DisplayName("打率が0から1の範囲外なら拒否する")
    void rejectsHitAverageOutOfRange() {
        // given
        // when
        IllegalArgumentException negative =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> batter(1, -0.1f, PlayerPersonality.DEFAULT));
        IllegalArgumentException nan =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> batter(1, Float.NaN, PlayerPersonality.DEFAULT));
        IllegalArgumentException over =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> batter(1, 1.1f, PlayerPersonality.DEFAULT));

        // then
        assertAll(
                () -> assertEquals("hitAverage must be between 0.0 and 1.0", negative.getMessage()),
                () -> assertEquals("hitAverage must be between 0.0 and 1.0", nan.getMessage()),
                () -> assertEquals("hitAverage must be between 0.0 and 1.0", over.getMessage()));
    }

    @Test
    @DisplayName("性格がnullなら拒否する")
    void rejectsNullPersonality() {
        // given
        // when
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> batter(1, 0.3f, null));

        // then
        assertAll(() -> assertEquals("personality must not be null", exception.getMessage()));
    }
}
