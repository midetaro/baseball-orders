package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.BaseTransition;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.Stealable;

/** 盗塁、バント、打撃の順で一打席を進行するドメインサービス。 */
final class AtBatProcessor {

    void process(GameBattingContext context, BatterEntity batter) {

        // 盗塁
        trySteal(context);

        // バント
        if (applyBuntResult(context, buntResult(context, batter))) {
            return; // バントした場合は終了
        }

        // ヒッティング
        BattingResult battingResult = batter.swing(context.getCurrentBaseState().runnerCount());

        BasesState nextBaseState =
                switch (battingResult) {
                    case OUT -> {
                        context.addOutCounts(1);
                        yield context.getCurrentBaseState();
                    }
                    case HIT_SINGLE ->
                            applyTransition(
                                    context, context.getCurrentBaseState().hitSingle(batter));
                    case HIT_DOUBLE ->
                            applyTransition(
                                    context, context.getCurrentBaseState().hitDouble(batter));
                    case HIT_TRIPLE ->
                            applyTransition(
                                    context, context.getCurrentBaseState().hitTriple(batter));
                    case HIT_HOMER ->
                            applyTransition(context, context.getCurrentBaseState().hitHomer());
                };
        context.replaceBaseState(nextBaseState);
    }

    private BuntResult buntResult(GameBattingContext context, BatterEntity batter) {
        return context.getCurrentBaseState()
                .buntOpportunityByBase()
                .map(_ -> batter.bunt(context.getOutCount(), context.getCurrentBaseState()))
                .orElse(BuntResult.NOT_TRY);
    }

    private void trySteal(GameBattingContext context) {

        context.getCurrentBaseState()
                .stealOpportunity()
                .ifPresent(
                        opportunity -> {
                            switch (stealResult(opportunity)) {
                                case NOT_TRY -> {}
                                case FAILURE -> {
                                    context.addOutCounts(1);
                                    applyTransition(
                                            context,
                                            context.getCurrentBaseState()
                                                    .caughtStealing(opportunity));
                                }
                                case SUCCESS ->
                                        applyTransition(
                                                context,
                                                context.getCurrentBaseState()
                                                        .succeedSteal(opportunity));
                            }
                        });
    }

    private StealResult stealResult(Stealable stealable) {
        return switch (stealable.targetBase()) {
            case FIRST -> throw new IllegalArgumentException("盗塁先は二塁または三塁である必要があります");
            case SECOND -> stealable.runner().stealToDouble();
            case THIRD -> stealable.runner().stealToTriple();
        };
    }

    private boolean applyBuntResult(GameBattingContext context, BuntResult buntResult) {
        return switch (buntResult) {
            case NOT_TRY -> false;
            case FAILURE -> {
                context.addOutCounts(1);
                yield true;
            }
            case SUCCESS -> {
                applyTransition(context, context.getCurrentBaseState().sacrificeBunt());
                context.addOutCounts(1);
                yield true;
            }
        };
    }

    private BasesState applyTransition(GameBattingContext context, BaseTransition transition) {
        context.addScore(transition.scoredRuns());
        return transition.nextState();
    }
}
