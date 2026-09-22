package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.game.capability.StealableToDoubleBase;
import com.example.baseballorders.simulator.domain.game.capability.StealableToTripleBase;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import lombok.RequiredArgsConstructor;

/** 試合とイニング状態を共有するStateの共通基底実装。 */
@RequiredArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class AbstractBasesState {

    private static final float ADVANCE_FROM_FIRST_PROBABILITY = 0.2f;
    private static final float ADVANCE_FROM_SECOND_PROBABILITY = 0.2f;
    private static final float ADVANCE_FROM_THIRD_PROBABILITY = 0.1f;

    protected final InningStateContext context;

    public final BatterEntity runnerAt(Base base) {
        return context.runnerAt(base);
    }

    public final OutCount getOutCount() {
        return context.outCount();
    }

    public final int runnerCount() {
        return (isOccupied(Base.FIRST) ? 1 : 0)
                + (isOccupied(Base.SECOND) ? 1 : 0)
                + (isOccupied(Base.THIRD) ? 1 : 0);
    }

    private boolean isOccupied(Base base) {
        return runnerAt(base) != null;
    }

    public final void out() {
        context.addOut();
        boolean completed =
                switch (getOutCount()) {
                    case NO_OUT, ONE_OUT, TWO_OUT -> false;
                    case THREE_OUT -> true;
                };
        if (completed) {
            context.reset();
            context.changeState(0);
            context.completeInning();
        }
    }

    /** 打撃による凡退を適用し、三死目でなければ塁ごとの確率で先頭走者だけを進める。 */
    public final void battingOut() {
        Base leadRunnerBase = leadRunnerBase();
        boolean canAdvance =
                switch (getOutCount()) {
                    case NO_OUT, ONE_OUT -> true;
                    case TWO_OUT, THREE_OUT -> false;
                };
        if (canAdvance
                && leadRunnerBase != null
                && RandomGenerator.nextFloat() < advancementProbability(leadRunnerBase)) {
            advanceLeadRunner(leadRunnerBase);
        }
        out();
    }

    protected final BuntResult attemptBunt(BatterEntity batter, BuntType buntType) {
        return batter.bunt(context.outCount(), buntType);
    }

    /**
     * 全てのconcreteクラスで同じ処理をするので一律で定義
     *
     * <p>templateパターン
     *
     * @param batter 打者
     */
    protected final void applyHitTriple(BatterEntity batter) {
        transition(null, null, batter, runnerCount());
    }

    /**
     * 全てのconcreteクラスで同じ処理をするので一律で定義
     *
     * <p>templateパターン
     */
    protected final void applyHitHomer() {
        transition(null, null, null, runnerCount() + 1L);
    }

    protected final StealResult attemptStealToDouble() {
        return this instanceof StealableToDoubleBase stealable
                ? stealable.runner().stealToDouble()
                : StealResult.NOT_TRY;
    }

    protected final StealResult attemptStealToTriple() {
        return this instanceof StealableToTripleBase stealable
                ? stealable.runner().stealToTriple()
                : StealResult.NOT_TRY;
    }

    /** 進塁バントの対象走者を一つ先の塁へ進める。 */
    public final void advanceRunnersByBunt() {
        transition(null, runnerAt(Base.FIRST), runnerAt(Base.SECOND), 0);
    }

    /** スクイズ失敗でアウトになった三塁走者を取り除く。 */
    public final void retireRunnerOnThird() {
        transition(runnerAt(Base.FIRST), runnerAt(Base.SECOND), null, 0);
    }

    /** スクイズ成功で三塁走者を生還させる。 */
    public final void scoreRunnerOnThird() {
        transition(runnerAt(Base.FIRST), runnerAt(Base.SECOND), null, 1);
    }

    public final void stealNotTry() {}

    public final void stealFailure() {
        if (this instanceof StealableToDoubleBase) {
            transition(null, runnerAt(Base.SECOND), runnerAt(Base.THIRD), 0);
        } else {
            transition(runnerAt(Base.FIRST), null, runnerAt(Base.THIRD), 0);
        }
        context.out();
    }

    public final void stealSuccess() {
        if (this instanceof StealableToDoubleBase) {
            transition(null, runnerAt(Base.FIRST), runnerAt(Base.THIRD), 0);
        } else {
            transition(runnerAt(Base.FIRST), null, runnerAt(Base.SECOND), 0);
        }
    }

    protected final void transition(
            BatterEntity first, BatterEntity second, BatterEntity third, long runs) {
        context.place(first, second, third);
        context.addScore(runs);
        int configuration =
                (first == null ? 0 : 1) | (second == null ? 0 : 2) | (third == null ? 0 : 4);
        context.changeState(configuration);
    }

    private Base leadRunnerBase() {
        if (isOccupied(Base.THIRD)) {
            return Base.THIRD;
        }
        if (isOccupied(Base.SECOND)) {
            return Base.SECOND;
        }
        return isOccupied(Base.FIRST) ? Base.FIRST : null;
    }

    private float advancementProbability(Base base) {
        return switch (base) {
            case FIRST -> ADVANCE_FROM_FIRST_PROBABILITY;
            case SECOND -> ADVANCE_FROM_SECOND_PROBABILITY;
            case THIRD -> ADVANCE_FROM_THIRD_PROBABILITY;
        };
    }

    private void advanceLeadRunner(Base base) {
        RunnerAdvance advance =
                switch (base) {
                    case FIRST -> new RunnerAdvance(null, runnerAt(Base.FIRST), null, 0);
                    case SECOND ->
                            new RunnerAdvance(runnerAt(Base.FIRST), null, runnerAt(Base.SECOND), 0);
                    case THIRD ->
                            new RunnerAdvance(runnerAt(Base.FIRST), runnerAt(Base.SECOND), null, 1);
                };
        transition(advance.first(), advance.second(), advance.third(), advance.runs());
    }

    private record RunnerAdvance(
            BatterEntity first, BatterEntity second, BatterEntity third, long runs) {}
}
