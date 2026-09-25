package com.example.baseballorders.simulator.domain.player.strategy;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.BatterTestData;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 名前付き乱数定数が基準打者に対して名前どおりの結果になることを検証する。
 *
 * <p>確率仕様が変わった瞬間にここが赤くなり、L3 / L4 のシナリオが「意味が変わったのに緑のまま」になる事故を防ぐ。
 *
 * <p>戦略へ渡される定数はここで固定する。{@code Draws#ADVANCE} / {@code Draws#NO_ADVANCE} は戦略ではなく
 * 塁状態の凡退進塁判定が消費するため、{@code BattedOutAdvanceInningScenarioTest} が本番経路で両分岐を固定する。
 */
class DrawsSelfTest {

    static Stream<Arguments> battingDraws() {
        return Stream.of(
                arguments("WALKは四球になる", Draws.WALK, BattingResult.WALK),
                arguments("SINGLEは単打になる", Draws.SINGLE, BattingResult.HIT_SINGLE),
                arguments("DOUBLEは二塁打になる", Draws.DOUBLE, BattingResult.HIT_DOUBLE),
                arguments("TRIPLEは三塁打になる", Draws.TRIPLE, BattingResult.HIT_TRIPLE),
                arguments("HOMERは本塁打になる", Draws.HOMER, BattingResult.HIT_HOMER),
                arguments("STRIKEOUTは三振になる", Draws.STRIKEOUT, BattingResult.STRIKEOUT),
                arguments("BATTED_OUTは凡退になる", Draws.BATTED_OUT, BattingResult.BATTED_OUT));
    }

    static Stream<Arguments> buntDraws() {
        return Stream.of(
                arguments("BUNT_SUCCESSはバント成功になる", Draws.BUNT_SUCCESS, BuntResult.SUCCESS),
                arguments("BUNT_FAILUREはバント失敗になる", Draws.BUNT_FAILURE, BuntResult.FAILURE));
    }

    static Stream<Arguments> stealToSecondDraws() {
        return Stream.of(
                arguments(
                        "STEAL_TO_SECOND_NOT_TRYは二盗を試みない",
                        Draws.STEAL_TO_SECOND_NOT_TRY,
                        StealResult.NOT_TRY),
                arguments(
                        "STEAL_TO_SECOND_SUCCESSは二盗成功になる",
                        Draws.STEAL_TO_SECOND_SUCCESS,
                        StealResult.SUCCESS),
                arguments(
                        "STEAL_TO_SECOND_FAILUREは二盗失敗になる",
                        Draws.STEAL_TO_SECOND_FAILURE,
                        StealResult.FAILURE));
    }

    static Stream<Arguments> stealToThirdDraws() {
        return Stream.of(
                arguments(
                        "STEAL_TO_THIRD_NOT_TRYは三盗を試みない",
                        Draws.STEAL_TO_THIRD_NOT_TRY,
                        StealResult.NOT_TRY),
                arguments(
                        "STEAL_TO_THIRD_SUCCESSは三盗成功になる",
                        Draws.STEAL_TO_THIRD_SUCCESS,
                        StealResult.SUCCESS),
                arguments(
                        "STEAL_TO_THIRD_FAILUREは三盗失敗になる",
                        Draws.STEAL_TO_THIRD_FAILURE,
                        StealResult.FAILURE));
    }

    @DisplayName("打撃の名前付き乱数は基準打者で名前どおりの打席結果になる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("battingDraws")
    void mapsBattingDrawToResult(String description, float draw, BattingResult expected) {
        // given
        var batter = BatterTestData.referenceBatter();

        // when
        BattingResult result;
        try (ScriptedRandom random = ScriptedRandom.of(draw)) {
            result = batter.swing(0);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expected, result),
                    () -> random.assertFullyConsumed());
        }
    }

    @DisplayName("バントの名前付き乱数は基準打者で名前どおりのバント結果になる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("buntDraws")
    void mapsBuntDrawToResult(String description, float draw, BuntResult expected) {
        // given
        var batter = BatterTestData.referenceBatter();

        // when
        BuntResult result;
        try (ScriptedRandom random = ScriptedRandom.of(draw)) {
            result = batter.bunt(OutCount.NO_OUT, BuntType.ADVANCING);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expected, result),
                    () -> random.assertFullyConsumed());
        }
    }

    @DisplayName("二盗の名前付き乱数は基準打者で名前どおりの盗塁結果になる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("stealToSecondDraws")
    void mapsStealToSecondDrawToResult(String description, float draw, StealResult expected) {
        // given
        var runner = BatterTestData.referenceBatter();

        // when
        StealResult result;
        try (ScriptedRandom random = ScriptedRandom.of(draw)) {
            result = runner.stealToDouble();

            // then
            assertAll(
                    description,
                    () -> assertEquals(expected, result),
                    () -> random.assertFullyConsumed());
        }
    }

    @DisplayName("三盗の名前付き乱数は基準打者で名前どおりの盗塁結果になる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("stealToThirdDraws")
    void mapsStealToThirdDrawToResult(String description, float draw, StealResult expected) {
        // given
        var runner = BatterTestData.referenceBatter();

        // when
        StealResult result;
        try (ScriptedRandom random = ScriptedRandom.of(draw)) {
            result = runner.stealToTriple();

            // then
            assertAll(
                    description,
                    () -> assertEquals(expected, result),
                    () -> random.assertFullyConsumed());
        }
    }
}
