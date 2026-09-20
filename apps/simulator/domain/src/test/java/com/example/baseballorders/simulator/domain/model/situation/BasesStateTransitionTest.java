package com.example.baseballorders.simulator.domain.model.situation;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.BatterTestDataFactory;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.base.*;
import com.example.baseballorders.simulator.domain.model.base.capability.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.model.base.capability.Buntable;
import com.example.baseballorders.simulator.domain.model.base.capability.SqueezeBuntable;
import com.example.baseballorders.simulator.domain.model.base.capability.Stealable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BasesStateTransitionTest {
    private static final List<BatterEntity> BATTERS = BatterTestDataFactory.mock();
    private static final BatterEntity FIRST = BATTERS.get(0);
    private static final BatterEntity SECOND = BATTERS.get(1);
    private static final BatterEntity THIRD = BATTERS.get(2);
    private static final BatterEntity BATTER = BATTERS.get(3);
    private static final List<Class<? extends BasesState>> TYPES =
            List.of(
                    NoBasesState.class,
                    SingleBasesState.class,
                    DoubleBaseState.class,
                    FirstDoubleBaseState.class,
                    ThirdBaseState.class,
                    FirstThirdBaseState.class,
                    DoubleThirdBaseState.class,
                    FullBasesState.class);

    @ParameterizedTest(name = "配置{0}・{1}死・{2}")
    @MethodSource("events")
    @DisplayName("全走者配置の各結果イベントが走者・アウト・得点・次Stateを更新する")
    void appliesEvent(int mask, int outs, Event event) {
        // given
        var context = context(mask, outs);
        var before = context.getCurrentState();
        var expected = expected(mask, event);
        boolean reset = expected.outs() == 1 && outs == 2;
        int expectedMask = reset ? 0 : expected.mask();

        // when
        if (expected.mask() < 0) {
            var exception = assertThrows(IllegalStateException.class, () -> event.apply(context));
            // then
            assertAll(
                    () -> assertFalse(exception.getMessage().isBlank()),
                    () -> assertSame(before, context.getCurrentState()),
                    () -> assertEquals(OutCount.values()[outs], before.getOutCount()),
                    () -> assertEquals(0, context.getTotalScore()),
                    () -> assertSame((mask & 1) != 0 ? FIRST : null, before.runnerAt(Base.FIRST)),
                    () -> assertSame((mask & 2) != 0 ? SECOND : null, before.runnerAt(Base.SECOND)),
                    () -> assertSame((mask & 4) != 0 ? THIRD : null, before.runnerAt(Base.THIRD)));
            return;
        }
        event.apply(context);

        // then
        var after = context.getCurrentState();
        assertAll(
                () -> assertInstanceOf(TYPES.get(expectedMask), after),
                () ->
                        assertEquals(
                                reset ? OutCount.NO_OUT : OutCount.values()[outs + expected.outs()],
                                after.getOutCount()),
                () -> assertEquals(expected.score(), context.getTotalScore()),
                () -> assertEquals(reset ? 2 : 1, context.getInning()),
                () -> assertEquals(Integer.bitCount(expectedMask), after.runnerCount()),
                () -> assertSame(reset ? null : expected.first(), after.runnerAt(Base.FIRST)),
                () -> assertSame(reset ? null : expected.second(), after.runnerAt(Base.SECOND)),
                () -> assertSame(reset ? null : expected.third(), after.runnerAt(Base.THIRD)));
    }

    static Stream<Arguments> events() {
        return IntStream.range(0, 8)
                .boxed()
                .flatMap(
                        mask ->
                                IntStream.range(0, 3)
                                        .boxed()
                                        .flatMap(
                                                outs ->
                                                        Arrays.stream(Event.values())
                                                                .map(
                                                                        event ->
                                                                                arguments(
                                                                                        mask, outs,
                                                                                        event))));
    }

    private static Expected expected(int mask, Event event) {
        BatterEntity first = (mask & 1) != 0 ? FIRST : null;
        BatterEntity second = (mask & 2) != 0 ? SECOND : null;
        BatterEntity third = (mask & 4) != 0 ? THIRD : null;
        return switch (event) {
            case OUT -> new Expected(mask, 1, 0, first, second, third);
            case SINGLE ->
                    new Expected(
                            new int[] {1, 3, 5, 7, 1, 3, 5, 7}[mask],
                            0,
                            new int[] {0, 0, 0, 0, 1, 1, 1, 1}[mask],
                            BATTER,
                            first,
                            second);
            case DOUBLE ->
                    new Expected(
                            new int[] {2, 6, 2, 6, 2, 6, 2, 6}[mask],
                            0,
                            new int[] {0, 0, 1, 1, 1, 1, 2, 2}[mask],
                            null,
                            BATTER,
                            first);
            case TRIPLE -> new Expected(4, 0, Integer.bitCount(mask), null, null, BATTER);
            case HOMER -> new Expected(0, 0, Integer.bitCount(mask) + 1, null, null, null);
            case BUNT_NOT_TRY, STEAL_NOT_TRY -> new Expected(mask, 0, 0, first, second, third);
            case BUNT_FAILURE ->
                    new Expected(
                            new int[] {-1, 1, 2, 3, -1, -1, -1, -1}[mask],
                            1,
                            0,
                            first,
                            second,
                            third);
            case BUNT_SUCCESS ->
                    new Expected(
                            new int[] {-1, 2, 4, 6, -1, -1, -1, -1}[mask],
                            1,
                            0,
                            null,
                            first,
                            second);
            case STEAL_FAILURE ->
                    new Expected(
                            new int[] {-1, 0, 0, 1, -1, 4, -1, -1}[mask],
                            1,
                            0,
                            mask == 3 ? first : null,
                            null,
                            third);
            case STEAL_SUCCESS ->
                    new Expected(
                            new int[] {-1, 2, 4, 5, -1, 6, -1, -1}[mask],
                            0,
                            0,
                            mask == 3 ? first : null,
                            mask == 1 || mask == 5 ? first : null,
                            mask == 2 || mask == 3 ? second : third);
        };
    }

    private record Expected(
            int mask,
            int outs,
            long score,
            BatterEntity first,
            BatterEntity second,
            BatterEntity third) {}

    @ParameterizedTest(name = "配置{0}")
    @MethodSource("configurations")
    @DisplayName("各配置のStateは試合内で再利用され試合間では共有されない")
    void reusesStateWithinGame(int mask) {
        // given
        var context = context(mask, 1);
        var original = context.getCurrentState();
        var other = context(mask, 0);
        // when
        context.hitHomer();
        seed(context, mask);
        other.out();
        // then
        assertAll(
                () -> assertSame(original, context.getCurrentState()),
                () -> assertNotSame(original, other.getCurrentState()),
                () -> assertEquals(OutCount.ONE_OUT, original.getOutCount()),
                () -> assertEquals(Integer.bitCount(mask), original.runnerCount()),
                () -> assertEquals(Integer.bitCount(mask) + 1, context.getTotalScore()),
                () -> assertEquals(0, other.getTotalScore()));
    }

    static IntStream configurations() {
        return IntStream.range(0, 8);
    }

    @ParameterizedTest(name = "配置{0}")
    @MethodSource("configurations")
    @DisplayName("走者配置に対応する盗塁と犠打の能力を返す")
    void exposesOpportunities(int mask) {
        // given
        var state = context(mask, 0).getCurrentState();
        // when
        Optional<Stealable> steal =
                state instanceof Stealable stealable ? Optional.of(stealable) : Optional.empty();
        var bunt = state instanceof AdvancingBuntable;
        // then
        assertAll(
                () -> assertEquals(List.of(1, 2, 3, 5).contains(mask), steal.isPresent()),
                () -> assertEquals(List.of(1, 2, 3).contains(mask), bunt),
                () ->
                        steal.ifPresent(
                                opportunity -> {
                                    assertEquals(
                                            mask == 1 || mask == 5 ? Base.FIRST : Base.SECOND,
                                            opportunity.sourceBase());
                                    assertEquals(
                                            mask == 1 || mask == 5 ? Base.SECOND : Base.THIRD,
                                            opportunity.targetBase());
                                    assertSame(
                                            mask == 1 || mask == 5 ? FIRST : SECOND,
                                            opportunity.runner());
                                }));
    }

    @Test
    @DisplayName("盗塁と犠打の能力は許可された派生型に閉じている")
    void capabilitiesAreSealed() {
        // given
        var expected = List.of(SqueezeBuntable.class, AdvancingBuntable.class);
        // when
        var permitted = List.of(Buntable.class.getPermittedSubclasses());
        // then
        assertAll(
                () -> assertTrue(Stealable.class.isSealed()),
                () -> assertTrue(Buntable.class.isSealed()),
                () -> assertEquals(2, permitted.size()),
                () -> assertTrue(permitted.containsAll(expected)));
    }

    private static GameBattingContext context(int mask, int outs) {
        return GameStateTestFixture.context(
                (mask & 1) != 0 ? FIRST : null,
                (mask & 2) != 0 ? SECOND : null,
                (mask & 4) != 0 ? THIRD : null,
                OutCount.values()[outs]);
    }

    private static void seed(GameBattingContext context, int mask) {
        switch (mask) {
            case 0 -> {}
            case 1 -> context.hitSingle(FIRST);
            case 2 -> context.hitDouble(SECOND);
            case 3 -> {
                context.hitSingle(SECOND);
                context.hitSingle(FIRST);
            }
            case 4 -> context.hitTriple(THIRD);
            case 5 -> {
                context.hitDouble(THIRD);
                context.hitSingle(FIRST);
            }
            case 6 -> {
                context.hitSingle(THIRD);
                context.hitDouble(SECOND);
            }
            case 7 -> {
                context.hitSingle(THIRD);
                context.hitSingle(SECOND);
                context.hitSingle(FIRST);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    private enum Event {
        OUT,
        SINGLE,
        DOUBLE,
        TRIPLE,
        HOMER,
        BUNT_NOT_TRY,
        BUNT_FAILURE,
        BUNT_SUCCESS,
        STEAL_NOT_TRY,
        STEAL_FAILURE,
        STEAL_SUCCESS;

        void apply(GameBattingContext context) {
            switch (this) {
                case OUT -> context.out();
                case SINGLE -> context.hitSingle(BATTER);
                case DOUBLE -> context.hitDouble(BATTER);
                case TRIPLE -> context.hitTriple(BATTER);
                case HOMER -> context.hitHomer();
                case BUNT_NOT_TRY -> context.buntNotTry();
                case BUNT_FAILURE -> context.buntFailure();
                case BUNT_SUCCESS -> context.buntSuccess();
                case STEAL_NOT_TRY -> context.stealNotTry();
                case STEAL_FAILURE -> context.stealFailure();
                case STEAL_SUCCESS -> context.stealSuccess();
            }
        }
    }
}
