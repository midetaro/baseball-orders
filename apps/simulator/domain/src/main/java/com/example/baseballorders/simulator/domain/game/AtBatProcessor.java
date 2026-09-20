package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.game.capability.Stealable;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.BatterEntity;

/** プレー結果を取得し、対応するStateイベントをContextへ送る。 */
final class AtBatProcessor {

    boolean process(GameBattingContext context, BatterEntity batter) {

        long inningBeforeSteal = context.getInning();

        if (context.getCurrentState() instanceof Stealable stealable) {
            StealResult stealResult =
                    switch (stealable.targetBase()) {
                        case FIRST -> throw new IllegalStateException("一塁への盗塁はサポートされていません");
                        case SECOND -> stealable.stealToDouble();
                        case THIRD -> stealable.stealToTriple();
                    };

            switch (stealResult) {
                case NOT_TRY -> context.stealNotTry();
                case FAILURE -> context.stealFailure();
                case SUCCESS -> context.stealSuccess();
            }
        }

        if (context.isGameOver() || context.getInning() != inningBeforeSteal) {
            return false;
        }

        if (context.isBuntable()) {
            boolean bunted =
                    switch (batter.bunt(context.getCurrentState().getOutCount())) {
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
}
