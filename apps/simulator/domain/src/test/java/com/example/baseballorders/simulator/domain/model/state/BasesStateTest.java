package com.example.baseballorders.simulator.domain.model.state;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BasesStateTest {

    private static final BatterEntity FIRST_RUNNER = batter("一塁走者");
    private static final BatterEntity SECOND_RUNNER = batter("二塁走者");
    private static final BatterEntity THIRD_RUNNER = batter("三塁走者");
    private static final BatterEntity BATTER = batter("打者");

    @DisplayName("塁状態と打撃結果に応じて走者を進めて得点を加算する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("hitTestCases")
    void advancesRunnersAndAddsScore(
            String description,
            BasesState state,
            boolean hasFirst,
            boolean hasSecond,
            boolean hasThird,
            Hit hit,
            long expectedScore,
            BatterEntity expectedFirst,
            BatterEntity expectedSecond,
            BatterEntity expectedThird) {
        // given
        GameBattingContext context = contextWithRunners(hasFirst, hasSecond, hasThird);

        // when
        hit.apply(state, context, BATTER);

        // then
        assertAll(
                () -> assertEquals(expectedScore, context.getTotalScore(), description),
                () ->
                        assertSame(
                                expectedFirst,
                                context.getRunnerOnFirstBase().orElse(null),
                                description),
                () ->
                        assertSame(
                                expectedSecond,
                                context.getRunnerOnSecondBase().orElse(null),
                                description),
                () ->
                        assertSame(
                                expectedThird,
                                context.getRunnerOnThirdBase().orElse(null),
                                description));
    }

    static Stream<Arguments> hitTestCases() {
        return Stream.of(
                arguments(
                        "走者なしで単打なら打者が一塁へ進む",
                        new NoBasesState(),
                        false,
                        false,
                        false,
                        Hit.SINGLE,
                        0,
                        BATTER,
                        null,
                        null),
                arguments(
                        "走者なしで二塁打なら打者が二塁へ進む",
                        new NoBasesState(),
                        false,
                        false,
                        false,
                        Hit.DOUBLE,
                        0,
                        null,
                        BATTER,
                        null),
                arguments(
                        "走者なしで三塁打なら打者が三塁へ進む",
                        new NoBasesState(),
                        false,
                        false,
                        false,
                        Hit.TRIPLE,
                        0,
                        null,
                        null,
                        BATTER),
                arguments(
                        "走者なしで本塁打なら1点入る",
                        new NoBasesState(),
                        false,
                        false,
                        false,
                        Hit.HOMER,
                        1,
                        null,
                        null,
                        null),
                arguments(
                        "一塁で単打なら一二塁になる",
                        new SingleBasesState(),
                        true,
                        false,
                        false,
                        Hit.SINGLE,
                        0,
                        BATTER,
                        FIRST_RUNNER,
                        null),
                arguments(
                        "一塁で二塁打なら二三塁になる",
                        new SingleBasesState(),
                        true,
                        false,
                        false,
                        Hit.DOUBLE,
                        0,
                        null,
                        BATTER,
                        FIRST_RUNNER),
                arguments(
                        "一塁で三塁打なら1点入り打者が三塁へ進む",
                        new SingleBasesState(),
                        true,
                        false,
                        false,
                        Hit.TRIPLE,
                        1,
                        null,
                        null,
                        BATTER),
                arguments(
                        "一塁で本塁打なら2点入る",
                        new SingleBasesState(),
                        true,
                        false,
                        false,
                        Hit.HOMER,
                        2,
                        null,
                        null,
                        null),
                arguments(
                        "二塁で単打なら一三塁になる",
                        new DoubleBaseState(),
                        false,
                        true,
                        false,
                        Hit.SINGLE,
                        0,
                        BATTER,
                        null,
                        SECOND_RUNNER),
                arguments(
                        "二塁で二塁打なら1点入り打者が二塁へ進む",
                        new DoubleBaseState(),
                        false,
                        true,
                        false,
                        Hit.DOUBLE,
                        1,
                        null,
                        BATTER,
                        null),
                arguments(
                        "二塁で三塁打なら1点入り打者が三塁へ進む",
                        new DoubleBaseState(),
                        false,
                        true,
                        false,
                        Hit.TRIPLE,
                        1,
                        null,
                        null,
                        BATTER),
                arguments(
                        "二塁で本塁打なら2点入る",
                        new DoubleBaseState(),
                        false,
                        true,
                        false,
                        Hit.HOMER,
                        2,
                        null,
                        null,
                        null),
                arguments(
                        "三塁で単打なら1点入り打者が一塁へ進む",
                        new ThirdBaseState(),
                        false,
                        false,
                        true,
                        Hit.SINGLE,
                        1,
                        BATTER,
                        null,
                        null),
                arguments(
                        "三塁で二塁打なら1点入り打者が二塁へ進む",
                        new ThirdBaseState(),
                        false,
                        false,
                        true,
                        Hit.DOUBLE,
                        1,
                        null,
                        BATTER,
                        null),
                arguments(
                        "三塁で三塁打なら1点入り打者が三塁へ進む",
                        new ThirdBaseState(),
                        false,
                        false,
                        true,
                        Hit.TRIPLE,
                        1,
                        null,
                        null,
                        BATTER),
                arguments(
                        "三塁で本塁打なら2点入る",
                        new ThirdBaseState(),
                        false,
                        false,
                        true,
                        Hit.HOMER,
                        2,
                        null,
                        null,
                        null),
                arguments(
                        "一二塁で単打なら満塁になる",
                        new FirstDoubleBaseState(),
                        true,
                        true,
                        false,
                        Hit.SINGLE,
                        0,
                        BATTER,
                        FIRST_RUNNER,
                        SECOND_RUNNER),
                arguments(
                        "一二塁で二塁打なら1点入り二三塁になる",
                        new FirstDoubleBaseState(),
                        true,
                        true,
                        false,
                        Hit.DOUBLE,
                        1,
                        null,
                        BATTER,
                        FIRST_RUNNER),
                arguments(
                        "一二塁で三塁打なら2点入り打者が三塁へ進む",
                        new FirstDoubleBaseState(),
                        true,
                        true,
                        false,
                        Hit.TRIPLE,
                        2,
                        null,
                        null,
                        BATTER),
                arguments(
                        "一二塁で本塁打なら3点入る",
                        new FirstDoubleBaseState(),
                        true,
                        true,
                        false,
                        Hit.HOMER,
                        3,
                        null,
                        null,
                        null),
                arguments(
                        "一三塁で単打なら1点入り一二塁になる",
                        new FirstThirdBaseState(),
                        true,
                        false,
                        true,
                        Hit.SINGLE,
                        1,
                        BATTER,
                        FIRST_RUNNER,
                        null),
                arguments(
                        "一三塁で二塁打なら1点入り二三塁になる",
                        new FirstThirdBaseState(),
                        true,
                        false,
                        true,
                        Hit.DOUBLE,
                        1,
                        null,
                        BATTER,
                        FIRST_RUNNER),
                arguments(
                        "一三塁で三塁打なら2点入り打者が三塁へ進む",
                        new FirstThirdBaseState(),
                        true,
                        false,
                        true,
                        Hit.TRIPLE,
                        2,
                        null,
                        null,
                        BATTER),
                arguments(
                        "一三塁で本塁打なら3点入る",
                        new FirstThirdBaseState(),
                        true,
                        false,
                        true,
                        Hit.HOMER,
                        3,
                        null,
                        null,
                        null),
                arguments(
                        "二三塁で単打なら1点入り一三塁になる",
                        new DoubleThirdBaseState(),
                        false,
                        true,
                        true,
                        Hit.SINGLE,
                        1,
                        BATTER,
                        null,
                        SECOND_RUNNER),
                arguments(
                        "二三塁で二塁打なら2点入り打者が二塁へ進む",
                        new DoubleThirdBaseState(),
                        false,
                        true,
                        true,
                        Hit.DOUBLE,
                        2,
                        null,
                        BATTER,
                        null),
                arguments(
                        "二三塁で三塁打なら2点入り打者が三塁へ進む",
                        new DoubleThirdBaseState(),
                        false,
                        true,
                        true,
                        Hit.TRIPLE,
                        2,
                        null,
                        null,
                        BATTER),
                arguments(
                        "二三塁で本塁打なら3点入る",
                        new DoubleThirdBaseState(),
                        false,
                        true,
                        true,
                        Hit.HOMER,
                        3,
                        null,
                        null,
                        null),
                arguments(
                        "満塁で単打なら1点入り満塁を維持する",
                        new FullBasesState(),
                        true,
                        true,
                        true,
                        Hit.SINGLE,
                        1,
                        BATTER,
                        FIRST_RUNNER,
                        SECOND_RUNNER),
                arguments(
                        "満塁で二塁打なら2点入り二三塁になる",
                        new FullBasesState(),
                        true,
                        true,
                        true,
                        Hit.DOUBLE,
                        2,
                        null,
                        BATTER,
                        FIRST_RUNNER),
                arguments(
                        "満塁で三塁打なら3点入り打者が三塁へ進む",
                        new FullBasesState(),
                        true,
                        true,
                        true,
                        Hit.TRIPLE,
                        3,
                        null,
                        null,
                        BATTER),
                arguments(
                        "満塁で本塁打なら4点入る",
                        new FullBasesState(),
                        true,
                        true,
                        true,
                        Hit.HOMER,
                        4,
                        null,
                        null,
                        null));
    }

    @Test
    @DisplayName("アウトになるとアウトカウントが1つ増える")
    void addsOneOut() {
        // given
        BasesState state = new NoBasesState();
        GameBattingContext context = contextWithRunners(false, false, false);

        // when
        state.out(context);

        // then
        assertAll(() -> assertEquals(OutCount.ONE_OUT, context.getOutCount()));
    }

    private static GameBattingContext contextWithRunners(
            boolean hasFirst, boolean hasSecond, boolean hasThird) {
        GameBattingContext context = new GameBattingContext(new LineUpEntity(List.of(BATTER)));
        context.setRunnerOnFirstBase(optionalRunner(hasFirst, FIRST_RUNNER));
        context.setRunnerOnSecondBase(optionalRunner(hasSecond, SECOND_RUNNER));
        context.setRunnerOnThirdBase(optionalRunner(hasThird, THIRD_RUNNER));
        return context;
    }

    private static Optional<BatterEntity> optionalRunner(boolean isPresent, BatterEntity runner) {
        return isPresent ? Optional.of(runner) : Optional.empty();
    }

    private static BatterEntity batter(String name) {
        return new BatterEntity(
                name,
                0.0f,
                0.0f,
                0.0f,
                (hitAverage, sluggish) -> BattingResult.OUT,
                new StealStrategyStub(),
                (successRate, outCounts, basesState) ->
                        com.example.baseballorders.simulator.domain.code.BuntResult.FAILURE);
    }

    private enum Hit {
        SINGLE {
            @Override
            void apply(BasesState state, GameBattingContext context, BatterEntity batter) {
                state.hitSingle(context, batter);
            }
        },
        DOUBLE {
            @Override
            void apply(BasesState state, GameBattingContext context, BatterEntity batter) {
                state.hitDouble(context, batter);
            }
        },
        TRIPLE {
            @Override
            void apply(BasesState state, GameBattingContext context, BatterEntity batter) {
                state.hitTriple(context, batter);
            }
        },
        HOMER {
            @Override
            void apply(BasesState state, GameBattingContext context, BatterEntity batter) {
                state.hitHomer(context, batter);
            }
        };

        abstract void apply(BasesState state, GameBattingContext context, BatterEntity batter);
    }

    private static final class StealStrategyStub
            implements com.example.baseballorders.simulator.domain.model.behavior.StealStrategy {

        @Override
        public StealResult runToDouble() {
            return StealResult.NOT_TRY;
        }

        @Override
        public StealResult runToTriple() {
            return StealResult.NOT_TRY;
        }
    }
}
