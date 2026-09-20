package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.base.BasesState;
import com.example.baseballorders.simulator.domain.model.base.capability.Stealable;

/** プレー結果を取得し、対応するStateイベントをContextへ送る。 */
final class AtBatProcessor {
    boolean process(GameBattingContext context, BatterEntity batter) {
        long inningBeforeSteal = context.getInning();
        var opportunity = context.getCurrentState().stealOpportunity();
        StealResult stealResult =
                opportunity
                        .map(_ -> stealResult(context.getCurrentState()))
                        .orElse(StealResult.NOT_TRY);
        switch (stealResult) {
            case NOT_TRY -> context.stealNotTry();
            case FAILURE -> context.stealFailure();
            case SUCCESS -> context.stealSuccess();
        }
        if (context.isGameOver() || context.getInning() != inningBeforeSteal) {
            return false;
        }

        BuntResult buntResult = context.getCurrentState().bunt(batter);
        boolean bunted =
                switch (buntResult) {
                    case NOT_TRY -> {
                        context.buntNotTry();
                        yield false;
                    }
                    case FAILURE -> {
                        context.buntFailure();
                        yield true;
                    }
                    case SUCCESS -> {
                        context.buntSuccess();
                        yield true;
                    }
                };
        if (bunted) {
            return true;
        }
        switch (batter.swing(context.getCurrentState().runnerCount())) {
            case OUT -> context.out();
            case HIT_SINGLE -> context.hitSingle(batter);
            case HIT_DOUBLE -> context.hitDouble(batter);
            case HIT_TRIPLE -> context.hitTriple(batter);
            case HIT_HOMER -> context.hitHomer();
        }
        return true;
    }

    private StealResult stealResult(BasesState state) {
        Stealable stealable = state.stealOpportunity().orElseThrow();
        return switch (stealable.targetBase()) {
            case FIRST -> throw new IllegalArgumentException("盗塁先は二塁または三塁である必要があります");
            case SECOND -> state.stealToDouble();
            case THIRD -> state.stealToTriple();
        };
    }
}
