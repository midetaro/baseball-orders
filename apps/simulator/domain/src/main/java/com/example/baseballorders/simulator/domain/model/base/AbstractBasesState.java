package com.example.baseballorders.simulator.domain.model.base;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.base.capability.Buntable;
import com.example.baseballorders.simulator.domain.model.base.capability.Stealable;
import com.example.baseballorders.simulator.domain.model.base.capability.StealableToDoubleBase;
import com.example.baseballorders.simulator.domain.model.base.capability.StealableToTripleBase;
import lombok.RequiredArgsConstructor;

/** 試合とイニング状態を共有するStateの共通基底実装。 */
@RequiredArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class AbstractBasesState {

    protected final GameBattingContext context;
    private final InningState inningState;

    public final BatterEntity runnerAt(Base base) {
        return inningState.runnerAt(base);
    }

    public final OutCount getOutCount() {
        return inningState.getOutCount();
    }

    public final int runnerCount() {
        return (isOccupied(Base.FIRST) ? 1 : 0)
                + (isOccupied(Base.SECOND) ? 1 : 0)
                + (isOccupied(Base.THIRD) ? 1 : 0);
    }

    public final boolean isOccupied(Base base) {
        return runnerAt(base) != null;
    }

    public final void out() {
        inningState.addOut();
        boolean completed =
                switch (getOutCount()) {
                    case NO_OUT, ONE_OUT, TWO_OUT -> false;
                    case THREE_OUT -> true;
                };
        if (completed) {
            inningState.reset();
            context.changeState(0);
            context.completeInning();
        }
    }

    protected final BuntResult attemptBunt(BasesState state, BatterEntity batter) {
        return batter.bunt(state);
    }

    protected final void applyHitTriple(BatterEntity batter) {
        transition(null, null, batter, runnerCount());
    }

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

    public final void buntNotTry() {}

    public final void buntFailure() {
        requireBunt();
        out();
    }

    public final void buntSuccess() {
        requireBunt();
        transition(null, runnerAt(Base.FIRST), runnerAt(Base.SECOND), 0);
        context.out();
    }

    public final void stealNotTry() {}

    public final void stealFailure() {
        Stealable opportunity = requireSteal();
        if (opportunity instanceof StealableToDoubleBase) {
            transition(null, runnerAt(Base.SECOND), runnerAt(Base.THIRD), 0);
        } else {
            transition(runnerAt(Base.FIRST), null, runnerAt(Base.THIRD), 0);
        }
        context.out();
    }

    public final void stealSuccess() {
        Stealable opportunity = requireSteal();
        if (opportunity instanceof StealableToDoubleBase) {
            transition(null, runnerAt(Base.FIRST), runnerAt(Base.THIRD), 0);
        } else {
            transition(runnerAt(Base.FIRST), null, runnerAt(Base.SECOND), 0);
        }
    }

    protected final void transition(
            BatterEntity first, BatterEntity second, BatterEntity third, long runs) {
        inningState.place(first, second, third);
        context.addScore(runs);
        int configuration =
                (first == null ? 0 : 1) | (second == null ? 0 : 2) | (third == null ? 0 : 4);
        context.changeState(configuration);
    }

    private void requireBunt() {
        if (!(this instanceof Buntable)) {
            throw new IllegalStateException("犠打機会がありません");
        }
    }

    private Stealable requireSteal() {
        if (this instanceof Stealable stealable) {
            return stealable;
        }
        throw new IllegalStateException("盗塁機会がありません");
    }
}
