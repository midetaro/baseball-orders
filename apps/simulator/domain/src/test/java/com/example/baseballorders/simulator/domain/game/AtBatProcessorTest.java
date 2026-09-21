package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.BuntStrategy;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class AtBatProcessorTest {

    @Test
    @DisplayName("三振では走者を進めずアウトだけを加算する")
    void strikeoutAddsOutWithoutAdvancingRunner() {
        // given
        var runner = runner();
        var batter = mock(BatterEntity.class);
        when(batter.bunt(OutCount.NO_OUT)).thenReturn(BuntResult.NOT_TRY);
        when(batter.swing(1)).thenReturn(BattingResult.STRIKEOUT);
        var context = GameStateTestFixture.context(runner, null, null, OutCount.NO_OUT);

        // when
        var completed = new AtBatProcessor().process(context, batter);

        // then
        assertAll(
                () -> assertTrue(completed),
                () -> assertEquals(OutCount.ONE_OUT, context.getCurrentState().getOutCount()),
                () -> assertSame(runner, context.getCurrentState().runnerAt(Base.FIRST)),
                () -> assertEquals(1, context.getCurrentState().runnerCount()));
    }

    @Test
    @DisplayName("バント機会がなければバントせず打撃する")
    void swingsWithoutBuntOpportunity() {
        // given
        var batter = batter(1.0f, BehaviorStrategies.standardBunt());
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));

        // when
        var completed = new AtBatProcessor().process(context, batter);

        // then
        assertAll(
                () -> assertTrue(completed),
                () -> assertEquals(OutCount.ONE_OUT, context.getCurrentState().getOutCount()),
                () -> assertFalse(context.isBuntable()));
    }

    @Test
    @DisplayName("無死でバントが成功すると走者を進めて打撃しない")
    void appliesSuccessfulBuntWithoutSwinging() {
        // given
        var runner = batter(0.0f, BehaviorStrategies.noBunt());
        var batter = batter(1.0f, BehaviorStrategies.standardBunt());
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));
        context.hitSingle(runner);

        // when
        var completed = new AtBatProcessor().process(context, batter);

        // then
        assertAll(
                () -> assertTrue(completed),
                () -> assertEquals(OutCount.ONE_OUT, context.getCurrentState().getOutCount()),
                () -> assertSame(runner, context.getCurrentState().runnerAt(Base.SECOND)),
                () -> assertEquals(1, context.getCurrentState().runnerCount()));
    }

    @Test
    @DisplayName("一死で積極的なバントが成功すると二死になり走者を進める")
    void appliesEagerBuntWithOneOut() {
        // given
        var runner = batter(0.0f, BehaviorStrategies.noBunt());
        var batter = batter(1.0f, BehaviorStrategies.eagerBunt());
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));
        context.out();
        context.hitSingle(runner);

        // when
        var completed = new AtBatProcessor().process(context, batter);

        // then
        assertAll(
                () -> assertTrue(completed),
                () -> assertEquals(OutCount.TWO_OUT, context.getCurrentState().getOutCount()),
                () -> assertSame(runner, context.getCurrentState().runnerAt(Base.SECOND)),
                () -> assertEquals(1, context.getCurrentState().runnerCount()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("advancementOnOutCases")
    @DisplayName("凡退時は先頭走者だけが塁ごとの確率で進塁する")
    void advancesOnlyLeadRunnerOnOut(
            String description,
            BatterEntity first,
            BatterEntity second,
            BatterEntity third,
            float advancementRandom,
            BatterEntity expectedFirst,
            BatterEntity expectedSecond,
            BatterEntity expectedThird,
            long expectedScore) {
        // given
        var batter = batter(0.0f, BehaviorStrategies.noBunt());
        var context = GameStateTestFixture.context(first, second, third, OutCount.NO_OUT);

        // when
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.99f, advancementRandom);
            new AtBatProcessor().process(context, batter);

            // then
            assertAll(
                    () -> assertEquals(OutCount.ONE_OUT, context.getCurrentState().getOutCount()),
                    () -> assertSame(expectedFirst, context.getCurrentState().runnerAt(Base.FIRST)),
                    () ->
                            assertSame(
                                    expectedSecond,
                                    context.getCurrentState().runnerAt(Base.SECOND)),
                    () -> assertSame(expectedThird, context.getCurrentState().runnerAt(Base.THIRD)),
                    () -> assertEquals(expectedScore, context.getTotalScore()),
                    () -> randomGenerator.verify(RandomGenerator::nextFloat, times(2)));
        }
    }

    static Stream<Arguments> advancementOnOutCases() {
        var first = runner();
        var second = runner();
        var third = runner();
        return Stream.of(
                arguments("一塁走者は乱数が20%未満なら二塁へ進む", first, null, null, 0.19f, null, first, null, 0),
                arguments("一塁走者は乱数が20%以上なら進まない", first, null, null, 0.20f, first, null, null, 0),
                arguments("二塁走者は乱数が20%未満なら三塁へ進む", null, second, null, 0.19f, null, null, second, 0),
                arguments("二塁走者は乱数が20%以上なら進まない", null, second, null, 0.20f, null, second, null, 0),
                arguments("三塁走者は乱数が10%未満なら生還する", null, null, third, 0.09f, null, null, null, 1),
                arguments("三塁走者は乱数が10%以上なら進まない", null, null, third, 0.10f, null, null, third, 0),
                arguments(
                        "一二塁では先頭の二塁走者だけが三塁へ進む", first, second, null, 0.19f, first, null, second, 0),
                arguments("一三塁では先頭の三塁走者だけが生還する", first, null, third, 0.09f, first, null, null, 1),
                arguments("二三塁では先頭の三塁走者だけが生還する", null, second, third, 0.09f, null, second, null, 1),
                arguments(
                        "満塁では先頭の三塁走者だけが生還する", first, second, third, 0.09f, first, second, null, 1));
    }

    @Test
    @DisplayName("走者なしの凡退では進塁判定をしない")
    void doesNotRollForAdvancementWithoutRunner() {
        // given
        var batter = batter(0.0f, BehaviorStrategies.noBunt());
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));

        // when
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.99f);
            new AtBatProcessor().process(context, batter);

            // then
            assertAll(
                    () -> assertEquals(OutCount.ONE_OUT, context.getCurrentState().getOutCount()),
                    () -> assertEquals(0, context.getCurrentState().runnerCount()),
                    () -> randomGenerator.verify(RandomGenerator::nextFloat, times(1)));
        }
    }

    @Test
    @DisplayName("三死目の凡退では走者の進塁判定をせずイニングを終了する")
    void doesNotAdvanceRunnerOnThirdOut() {
        // given
        var batter = batter(0.0f, BehaviorStrategies.noBunt());
        var context = GameStateTestFixture.context(runner(), null, null, OutCount.TWO_OUT);

        // when
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.99f);
            new AtBatProcessor().process(context, batter);

            // then
            assertAll(
                    () -> assertEquals(2, context.getInning()),
                    () -> assertEquals(OutCount.NO_OUT, context.getCurrentState().getOutCount()),
                    () -> assertEquals(0, context.getCurrentState().runnerCount()),
                    () -> assertEquals(0, context.getTotalScore()),
                    () -> randomGenerator.verify(RandomGenerator::nextFloat, times(1)));
        }
    }

    private static BatterEntity batter(float buntSuccessRate, BuntStrategy buntStrategy) {
        return new BatterEntity(
                0.0f,
                0.0f,
                buntSuccessRate,
                0.0f,
                BehaviorStrategies.middleDistanceHittingStrategy(),
                BehaviorStrategies.noSteal(),
                buntStrategy);
    }

    private static BatterEntity runner() {
        return batter(0.0f, BehaviorStrategies.noBunt());
    }
}
