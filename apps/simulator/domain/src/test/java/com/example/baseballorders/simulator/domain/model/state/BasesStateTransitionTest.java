package com.example.baseballorders.simulator.domain.model.state;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
        BasesState next = hit.apply(state, context);

        // then
        assertAll(
                () -> assertInstanceOf(expected, next, description),
                () -> assertEquals(score, context.getTotalScore(), description));
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

    @DisplayName("進塁と塁上走者の置換は次の状態を返す")
    @ParameterizedTest(name = "{0}")
    @MethodSource("advances")
    void advances(String description, Base base, Class<? extends BasesState> expected) {
        // given
        BasesState state = new FullBasesState(RUNNER, RUNNER, RUNNER);

        // when
        BasesState next = state.advance(base);

        // then
        assertAll(() -> assertInstanceOf(expected, next, description));
    }

    static Stream<Arguments> advances() {
        return Stream.of(
                arguments("一つ進塁", Base.FIRST, FirstDoubleBaseState.class),
                arguments("二つ進塁", Base.SECOND, ThirdBaseState.class),
                arguments("走者消去", Base.THIRD, NoBasesState.class));
    }

    private enum Hit {
        SINGLE {
            BasesState apply(BasesState state, GameBattingContext context) {
                return state.hitSingle(context, BATTER);
            }
        },
        DOUBLE {
            BasesState apply(BasesState state, GameBattingContext context) {
                return state.hitDouble(context, BATTER);
            }
        },
        TRIPLE {
            BasesState apply(BasesState state, GameBattingContext context) {
                return state.hitTriple(context, BATTER);
            }
        },
        HOMER {
            BasesState apply(BasesState state, GameBattingContext context) {
                return state.hitHomer(context, BATTER);
            }
        };

        abstract BasesState apply(BasesState state, GameBattingContext context);
    }
}
