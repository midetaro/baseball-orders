package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.game.capability.Buntable;
import com.example.baseballorders.simulator.domain.game.capability.Stealable;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.BatterEntity;

/** プレー結果を取得し、対応するStateイベントをContextへ送る。 */
final class AtBatProcessor {

    boolean process(InningStateContext context, BatterEntity batter) {

        long inningBeforeSteal = context.inning();
        // 盗塁フェーズ
        if (context.currentBaseState() instanceof Stealable stealable) {
            StealResult stealResult =
                    switch (stealable.targetBase()) {
                        case FIRST -> throw new IllegalStateException("一塁への盗塁はサポートされていません");
                        case SECOND -> stealable.stealToDouble();
                        case THIRD -> stealable.stealToTriple();
                    };

            switch (stealResult) {
                case NOT_TRY -> stealable.stealNotTry();
                case FAILURE -> stealable.stealFailure();
                case SUCCESS -> stealable.stealSuccess();
            }
        }

        if (context.isGameOver() || context.inning() != inningBeforeSteal) {
            return false;
        }

        // バントフェーズ
        if (context.currentBaseState() instanceof Buntable buntable) {
            switch (buntable.bunt(batter)) {
                case NOT_TRY -> buntable.buntNotTry();
                case FAILURE -> {
                    buntable.buntFailure();
                    return true;
                }
                case SUCCESS -> {
                    buntable.buntSuccess();
                    return true;
                }
            }
        }

        // 打撃フェーズ
        switch (batter.swing(context.currentBaseState().runnerCount())) {
            case STRIKEOUT -> context.currentBaseState().out();
            case BATTED_OUT -> context.currentBaseState().battingOut();
            case WALK -> context.currentBaseState().walk(batter);
            case HIT_SINGLE -> context.currentBaseState().hitSingle(batter);
            case HIT_DOUBLE -> context.currentBaseState().hitDouble(batter);
            case HIT_TRIPLE -> context.currentBaseState().hitTriple(batter);
            case HIT_HOMER -> context.currentBaseState().hitHomer();
        }
        return true;
    }
}
