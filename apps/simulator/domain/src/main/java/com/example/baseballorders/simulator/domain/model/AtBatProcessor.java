package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.base.capability.Buntable;
import com.example.baseballorders.simulator.domain.model.base.capability.Stealable;

/** プレー結果を取得し、対応するStateイベントをContextへ送る。 */
final class AtBatProcessor {

    boolean process(GameBattingContext context, BatterEntity batter) {

        long inningBeforeSteal = context.getInning();

        var stealable =
                context.getCurrentState() instanceof Stealable opportunity ? opportunity : null;

        StealResult stealResult = stealable == null ? StealResult.NOT_TRY : stealResult(stealable);
        switch (stealResult) {
            case NOT_TRY -> context.stealNotTry();
            case FAILURE -> context.stealFailure();
            case SUCCESS -> context.stealSuccess();
        }
        if (context.isGameOver() || context.getInning() != inningBeforeSteal) {
            return false;
        }

        var buntable =
                context.getCurrentState() instanceof Buntable opportunity ? opportunity : null;
        BuntResult buntResult = buntable == null ? BuntResult.NOT_TRY : buntable.bunt(batter);
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

    private StealResult stealResult(Stealable stealable) {
        return switch (stealable.targetBase()) {
            case FIRST -> throw new IllegalArgumentException("盗塁先は二塁または三塁である必要があります");
            case SECOND -> stealable.stealToDouble();
            case THIRD -> stealable.stealToTriple();
        };
    }
}
