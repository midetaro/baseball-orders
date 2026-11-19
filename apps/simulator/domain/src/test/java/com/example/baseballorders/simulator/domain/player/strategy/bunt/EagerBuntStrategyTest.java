package com.example.baseballorders.simulator.domain.player.strategy.bunt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.rule.BuntProbabilities;
import com.example.baseballorders.simulator.domain.rule.BuntProbabilitiesBuilder;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 積極バント戦略の確率仕様。
 *
 * <p>標準戦略との違いは一死でも試行することだけで、成功判定・企図判定の確率は同じ。設定された進塁バント成功率 0.700・スクイズ企図率 0.250・スクイズ成功率 0.450
 * では次のとおり。
 *
 * <pre>
 * 進塁バント(無死・一死) : [0.0000000, 0.7000000) 成功 / [0.7000000, 1.0000000] 失敗
 * 二死・三死            : 乱数を引かず NOT_TRY
 * </pre>
 */
class EagerBuntStrategyTest {

    private static final BuntProbabilities BUNT_PROBABILITIES =
            BuntProbabilitiesBuilder.buntProbabilities()
                    .advancingSuccessRate(0.700f)
                    .squeezeSuccessRate(0.450f)
                    .squeezeChallengeRate(0.250f)
                    .build();

    static Stream<Arguments> advancingBoundaries() {
        return Stream.of(
                arguments("無死・0.0000000は成功区間の下端", OutCount.NO_OUT, 0.0f, BuntResult.SUCCESS),
                arguments("無死・0.6999999は成功区間の直前", OutCount.NO_OUT, 0.6999999f, BuntResult.SUCCESS),
                arguments("無死・0.7000000は成功率と等しく失敗区間の下端", OutCount.NO_OUT, 0.7f, BuntResult.FAILURE),
                arguments("無死・1.0000000は失敗区間の上端", OutCount.NO_OUT, 1.0f, BuntResult.FAILURE),
                arguments("一死・0.0000000は成功区間の下端", OutCount.ONE_OUT, 0.0f, BuntResult.SUCCESS),
                arguments("一死・0.6999999は成功区間の直前", OutCount.ONE_OUT, 0.6999999f, BuntResult.SUCCESS),
                arguments(
                        "一死・0.7000000は成功率と等しく失敗区間の下端", OutCount.ONE_OUT, 0.7f, BuntResult.FAILURE),
                arguments("一死・1.0000000は失敗区間の上端", OutCount.ONE_OUT, 1.0f, BuntResult.FAILURE));
    }

    @DisplayName("無死と一死では乱数区間の境界どおりに進塁バント結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {3}")
    @MethodSource("advancingBoundaries")
    void determinesAdvancingBuntResultAtBoundary(
            String description, OutCount outCount, float random, BuntResult expectedResult) {
        // given
        var sut = new EagerBuntStrategy(BUNT_PROBABILITIES);

        // when
        BuntResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            result = sut.bunt(outCount, BuntType.ADVANCING);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }

    @DisplayName("一死のスクイズは企図ロールが企図率以上なら乱数を1個消費してNOT_TRYになる")
    @org.junit.jupiter.api.Test
    void doesNotChallengeSqueezeWhenChallengeRollAtOrAboveRate() {
        // given
        var sut = new EagerBuntStrategy(BUNT_PROBABILITIES);

        // when
        BuntResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.25f)) {
            result = sut.bunt(OutCount.ONE_OUT, BuntType.SQUEEZE);

            // then
            assertAll(
                    () -> assertEquals(BuntResult.NOT_TRY, result),
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }

    static Stream<Arguments> squeezeSuccessRollBoundaries() {
        return Stream.of(
                arguments("0.0000000は成功区間の下端", 0.0f, BuntResult.SUCCESS),
                arguments("0.4499999は成功区間の直前", 0.4499999f, BuntResult.SUCCESS),
                arguments("0.4500000は成功率と等しく失敗区間の下端", 0.45f, BuntResult.FAILURE),
                arguments("1.0000000は失敗区間の上端", 1.0f, BuntResult.FAILURE));
    }

    @DisplayName("一死のスクイズは企図ロールが企図率未満なら成否ロールへ進み、乱数を2個消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("squeezeSuccessRollBoundaries")
    void challengesSqueezeThenDeterminesSuccessRollWithOneOut(
            String description, float successRoll, BuntResult expectedResult) {
        // given
        var sut = new EagerBuntStrategy(BUNT_PROBABILITIES);

        // when
        BuntResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.0f, successRoll)) {
            result = sut.bunt(OutCount.ONE_OUT, BuntType.SQUEEZE);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    () -> assertEquals(2, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }

    @DisplayName("二死以降は乱数を引かずにバントを試行しない")
    @ParameterizedTest
    @EnumSource(
            value = OutCount.class,
            names = {"TWO_OUT", "THREE_OUT"})
    void neverAttemptsBuntAfterSecondOut(OutCount outCount) {
        // given
        var sut = new EagerBuntStrategy(BUNT_PROBABILITIES);

        // when
        BuntResult advancingResult;
        BuntResult squeezeResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of()) {
            advancingResult = sut.bunt(outCount, BuntType.ADVANCING);
            squeezeResult = sut.bunt(outCount, BuntType.SQUEEZE);

            // then
            assertAll(
                    () -> assertEquals(BuntResult.NOT_TRY, advancingResult),
                    () -> assertEquals(BuntResult.NOT_TRY, squeezeResult),
                    () -> assertEquals(0, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }

    static Stream<Arguments> oneOutComparisons() {
        return Stream.of(
                arguments("一死・0.6999999では積極戦略だけが成功する", 0.6999999f, BuntResult.SUCCESS),
                arguments("一死・0.7000000では積極戦略だけが失敗する", 0.7f, BuntResult.FAILURE));
    }

    @DisplayName("一死では標準戦略が試行しないのに積極戦略は試行する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("oneOutComparisons")
    void attemptsBuntWithOneOutUnlikeStandardStrategy(
            String description, float random, BuntResult expectedEagerResult) {
        // given
        var sut = new EagerBuntStrategy(BUNT_PROBABILITIES);
        var standardStrategy = new StandardBuntStrategy(BUNT_PROBABILITIES);

        // when
        BuntResult eagerResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            eagerResult = sut.bunt(OutCount.ONE_OUT, BuntType.ADVANCING);
            scriptedRandom.assertFullyConsumed();
        }
        BuntResult standardResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of()) {
            standardResult = standardStrategy.bunt(OutCount.ONE_OUT, BuntType.ADVANCING);
            scriptedRandom.assertFullyConsumed();
        }

        // then
        assertAll(
                description,
                () -> assertEquals(expectedEagerResult, eagerResult, "積極戦略は一死でも成否を判定すること"),
                () -> assertEquals(BuntResult.NOT_TRY, standardResult, "標準戦略は一死では試行しないこと"));
    }
}
