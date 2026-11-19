package com.example.baseballorders.simulator.domain.player.strategy.batting;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilitiesBuilder;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BattingResultSelectorTest {

    @Test
    @DisplayName("四球を除いた打席の安打割合が指定打率になり四球割合は打率に依存しない")
    void reproducesBattingAverageAcrossUniformDraws() {
        // given
        var sut =
                new BattingResultSelector(
                        BattingProbabilitiesBuilder.battingProbabilities()
                                .walkProbability(0.25f)
                                .strikeoutProbabilityWhenNotOnBase(0.25f)
                                .build());
        int walkCount = 0;
        int hitCount = 0;

        // when
        for (int index = 0; index < 256; index++) {
            BattingResult result = sut.select((index + 0.5f) / 256, 0.25f, 1, 0, 0, 0);
            if (result == BattingResult.WALK) {
                walkCount++;
            }
            if (result == BattingResult.HIT_SINGLE) {
                hitCount++;
            }
        }

        // then
        int observedWalks = walkCount;
        int observedHits = hitCount;
        assertAll(
                () -> assertEquals(64, observedWalks),
                () -> assertEquals(48, observedHits),
                () -> assertEquals(0.25f, (float) observedHits / (256 - observedWalks)));
    }

    static Stream<Arguments> extremeProbabilities() {
        return Stream.of(
                arguments(0f, 0f, 0f, BattingResult.STRIKEOUT),
                arguments(0f, 1f, Math.nextDown(1f), BattingResult.HIT_SINGLE),
                arguments(1f, 0f, Math.nextDown(1f), BattingResult.WALK),
                arguments(1f, 1f, Math.nextDown(1f), BattingResult.WALK),
                arguments(0.25f, 0f, Math.nextDown(0.25f), BattingResult.WALK),
                arguments(0.25f, 0f, 0.25f, BattingResult.STRIKEOUT),
                arguments(0.25f, 1f, 0.25f, BattingResult.HIT_SINGLE),
                arguments(0.25f, 1f, Math.nextDown(1f), BattingResult.HIT_SINGLE));
    }

    @DisplayName("打率と四球確率が0または1でも各確率の境界を守る")
    @ParameterizedTest
    @MethodSource("extremeProbabilities")
    void handlesExtremeProbabilities(
            float walkProbability, float battingAverage, float random, BattingResult expected) {
        // given
        var sut =
                new BattingResultSelector(
                        BattingProbabilitiesBuilder.battingProbabilities()
                                .walkProbability(walkProbability)
                                .strikeoutProbabilityWhenNotOnBase(0.25f)
                                .build());

        // when
        BattingResult result = sut.select(random, battingAverage, 1, 0, 0, 0);

        // then
        assertAll(() -> assertEquals(expected, result));
    }
}
