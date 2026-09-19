package com.example.baseballorders.simulator.domain.model.state;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.BatterTestDataFactory;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BasesStateTransitionTest {
    private static final BatterEntity RUNNER = BatterTestDataFactory.mock().getFirst();
    private static final BatterEntity BATTER = BatterTestDataFactory.mock().get(1);

    @DisplayName("走者を持つ塁状態は不変に次の状態へ遷移する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("transitions")
    void transitions(
            String description,
            BasesState state,
            Hit hit,
            Class<? extends BasesState> expected,
            long score) {
        // given
        GameBattingContext context =
                new GameBattingContext(new LineUpEntity(Collections.nCopies(9, BATTER)));

        // when
        BaseTransition transition = hit.apply(state, BATTER);

        // then
        assertAll(
                () -> assertInstanceOf(expected, transition.nextState(), description),
                () -> assertEquals(score, transition.scoredRuns(), description),
                () -> assertEquals(0, context.getTotalScore(), description));
    }

    static Stream<Arguments> transitions() {
        return Stream.of(
                arguments("走者なし単打", new NoBasesState(), Hit.SINGLE, SingleBasesState.class, 0),
                arguments(
                        "一塁二塁打",
                        new SingleBasesState(RUNNER),
                        Hit.DOUBLE,
                        DoubleThirdBaseState.class,
                        0),
                arguments(
                        "二塁三塁打", new DoubleBaseState(RUNNER), Hit.TRIPLE, ThirdBaseState.class, 1),
                arguments(
                        "三塁単打", new ThirdBaseState(RUNNER), Hit.SINGLE, SingleBasesState.class, 1),
                arguments(
                        "一二塁本塁打",
                        new FirstDoubleBaseState(RUNNER, RUNNER),
                        Hit.HOMER,
                        NoBasesState.class,
                        3),
                arguments(
                        "一三塁三塁打",
                        new FirstThirdBaseState(RUNNER, RUNNER),
                        Hit.TRIPLE,
                        ThirdBaseState.class,
                        2),
                arguments(
                        "二三塁二塁打",
                        new DoubleThirdBaseState(RUNNER, RUNNER),
                        Hit.DOUBLE,
                        DoubleBaseState.class,
                        2),
                arguments(
                        "満塁単打",
                        new FullBasesState(RUNNER, RUNNER, RUNNER),
                        Hit.SINGLE,
                        FullBasesState.class,
                        1));
    }

    @DisplayName("塁状態の打撃遷移は試合コンテキストを変更しない")
    @org.junit.jupiter.api.Test
    void hitTransitionDoesNotMutateGameContext() {
        // given
        BasesState state = new ThirdBaseState(RUNNER);
        GameBattingContext context =
                new GameBattingContext(new LineUpEntity(Collections.nCopies(9, BATTER)));

        // when
        BaseTransition transition = state.hitSingle(BATTER);

        // then
        assertAll(
                () -> assertEquals(1, transition.scoredRuns()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @DisplayName("成功犠打は走者を一つ進めた次の状態を返す")
    @ParameterizedTest(name = "{0}")
    @MethodSource("sacrificeBunts")
    void appliesSacrificeBunt(
            String description, BasesState state, Class<? extends BasesState> expected) {
        // given

        // when
        BaseTransition transition = state.sacrificeBunt();

        // then
        assertAll(
                () -> assertInstanceOf(expected, transition.nextState(), description),
                () -> assertEquals(0, transition.scoredRuns(), description));
    }

    static Stream<Arguments> sacrificeBunts() {
        return Stream.of(
                arguments("一塁走者", new SingleBasesState(RUNNER), DoubleBaseState.class),
                arguments(
                        "一二塁走者",
                        new FirstDoubleBaseState(RUNNER, RUNNER),
                        DoubleThirdBaseState.class));
    }

    @DisplayName("盗塁結果は走者配置だけを遷移させる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("stealTransitions")
    void appliesStealTransition(
            String description,
            BasesState state,
            StealResult result,
            Class<? extends BasesState> expected) {
        // given

        // when
        BaseTransition transition =
                switch (result) {
                    case SUCCESS -> state.succeedSteal(state.stealOpportunity().orElseThrow());
                    case FAILURE -> state.caughtStealing(state.stealOpportunity().orElseThrow());
                };

        // then
        assertAll(
                () -> assertInstanceOf(expected, transition.nextState(), description),
                () -> assertEquals(0, transition.scoredRuns(), description));
    }

    static Stream<Arguments> stealTransitions() {
        return Stream.of(
                arguments(
                        "一塁走者の盗塁成功",
                        new SingleBasesState(RUNNER),
                        StealResult.SUCCESS,
                        DoubleBaseState.class),
                arguments(
                        "二塁走者の盗塁死",
                        new DoubleBaseState(RUNNER),
                        StealResult.FAILURE,
                        NoBasesState.class));
    }

    @DisplayName("盗塁能力は許可した派生インタフェースだけを持つsealed型である")
    @org.junit.jupiter.api.Test
    void stealableIsSealed() {
        // given

        // when
        boolean sealed = Stealable.class.isSealed();

        // then
        assertAll(() -> assertTrue(sealed));
    }

    @DisplayName("盗塁候補は盗塁可能インタフェースを実装した塁状態だけが返す")
    @org.junit.jupiter.api.Test
    void findsStealOpportunityOnlyForStealableState() {
        // given
        BasesState state = new BasesState(RUNNER, null, null) {};

        // when
        var opportunity = state.stealOpportunity();

        // then
        assertAll(() -> assertEquals(false, opportunity.isPresent()));
    }

    @DisplayName("盗塁候補は空いている次の塁へ進む走者を返す")
    @ParameterizedTest(name = "{0}")
    @MethodSource("stealOpportunities")
    void findsStealOpportunity(
            String description, BasesState state, Base source, Base destination, boolean expected) {
        // given

        // when
        var opportunity = state.stealOpportunity();

        // then
        assertAll(
                () -> assertEquals(expected, opportunity.isPresent(), description),
                () -> {
                    if (expected) {
                        assertEquals(source, opportunity.orElseThrow().sourceBase(), description);
                        assertEquals(
                                destination, opportunity.orElseThrow().targetBase(), description);
                    }
                });
    }

    static Stream<Arguments> stealOpportunities() {
        return Stream.of(
                arguments("一塁走者", new SingleBasesState(RUNNER), Base.FIRST, Base.SECOND, true),
                arguments(
                        "一三塁走者",
                        new FirstThirdBaseState(RUNNER, RUNNER),
                        Base.FIRST,
                        Base.SECOND,
                        true),
                arguments("二塁走者", new DoubleBaseState(RUNNER), Base.SECOND, Base.THIRD, true),
                arguments(
                        "一二塁走者",
                        new FirstDoubleBaseState(RUNNER, RUNNER),
                        Base.SECOND,
                        Base.THIRD,
                        true),
                arguments("二三塁走者", new DoubleThirdBaseState(RUNNER, RUNNER), null, null, false),
                arguments("走者なし", new NoBasesState(), null, null, false));
    }

    @DisplayName("バント能力は許可した塁状態だけを持つsealed型である")
    @org.junit.jupiter.api.Test
    void buntableIsSealed() {
        // given

        // when
        boolean sealed = Buntable.class.isSealed();

        // then
        assertAll(() -> assertTrue(sealed));
    }

    @DisplayName("バント候補はバント可能インタフェースを実装した塁状態だけが返す")
    @ParameterizedTest(name = "{0}")
    @MethodSource("buntOpportunities")
    void findsBuntOpportunityByBase(String description, BasesState state, boolean expected) {
        // given

        // when
        var opportunity = state.buntOpportunityByBase();

        // then
        assertAll(() -> assertEquals(expected, opportunity.isPresent(), description));
    }

    static Stream<Arguments> buntOpportunities() {
        return Stream.of(
                arguments("一塁走者", new SingleBasesState(RUNNER), true),
                arguments("二塁走者", new DoubleBaseState(RUNNER), true),
                arguments("一二塁走者", new FirstDoubleBaseState(RUNNER, RUNNER), true),
                arguments("一三塁走者", new FirstThirdBaseState(RUNNER, RUNNER), false),
                arguments("二三塁走者", new DoubleThirdBaseState(RUNNER, RUNNER), false),
                arguments("満塁", new FullBasesState(RUNNER, RUNNER, RUNNER), false),
                arguments("走者なし", new NoBasesState(), false));
    }

    private enum Hit {
        SINGLE {
            BaseTransition apply(BasesState state, BatterEntity batter) {
                return state.hitSingle(batter);
            }
        },
        DOUBLE {
            BaseTransition apply(BasesState state, BatterEntity batter) {
                return state.hitDouble(batter);
            }
        },
        TRIPLE {
            BaseTransition apply(BasesState state, BatterEntity batter) {
                return state.hitTriple(batter);
            }
        },
        HOMER {
            BaseTransition apply(BasesState state, BatterEntity batter) {
                return state.hitHomer();
            }
        };

        abstract BaseTransition apply(BasesState state, BatterEntity batter);
    }

    private enum StealResult {
        SUCCESS,
        FAILURE
    }
}
