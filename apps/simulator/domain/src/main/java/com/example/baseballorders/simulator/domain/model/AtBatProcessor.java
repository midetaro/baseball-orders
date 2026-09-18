package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.StealableToDoubleBase;
import com.example.baseballorders.simulator.domain.model.state.StealableToTripleBase;
import com.example.baseballorders.simulator.domain.model.statistics.GameStatisticsRecorder;

/** 盗塁、バント、打撃の順で一打席を進行するドメインサービス。 */
final class AtBatProcessor {

    void process(
            GameBattingContext context,
            BatterEntity batter,
            GameStatisticsRecorder statisticsRecorder) {
        trySteal(context, statisticsRecorder);
        context.updateBaseStateOf();

        if (applyBuntResult(
                context,
                batter.bunt(context.getOutCount(), context.getCurrentBaseState()),
                statisticsRecorder)) {
            context.updateBaseStateOf();
            return;
        }

        BattingResult battingResult = batter.swing();
        if (battingResult == BattingResult.HIT_HOMER) {
            statisticsRecorder.recordHomeRun(context.getRunners().count());
        }
        Runnable applyBattingResult =
                switch (battingResult) {
                    case OUT -> () -> context.getCurrentBaseState().out(context);
                    case HIT_SINGLE ->
                            () -> context.getCurrentBaseState().hitSingle(context, batter);
                    case HIT_DOUBLE ->
                            () -> context.getCurrentBaseState().hitDouble(context, batter);
                    case HIT_TRIPLE ->
                            () -> context.getCurrentBaseState().hitTriple(context, batter);
                    case HIT_HOMER -> () -> context.getCurrentBaseState().hitHomer(context, batter);
                };
        applyBattingResult.run();
        context.updateBaseStateOf();
    }

    private boolean applyBuntResult(
            GameBattingContext context,
            BuntResult buntResult,
            GameStatisticsRecorder statisticsRecorder) {
        return switch (buntResult) {
            case NOT_TRY -> false;
            case FAILURE -> {
                context.addOutCounts(1);
                yield true;
            }
            case SUCCESS -> {
                statisticsRecorder.recordBunt();
                context.moveRunnerNthBase(Base.FIRST);
                context.addOutCounts(1);
                yield true;
            }
        };
    }

    private void trySteal(GameBattingContext context, GameStatisticsRecorder statisticsRecorder) {
        if (context.getCurrentBaseState() instanceof StealableToDoubleBase) {
            applySteal(
                    context,
                    Base.FIRST,
                    Base.SECOND,
                    context.getRunners().runnerAt(Base.FIRST).stealToDouble(),
                    statisticsRecorder);
        }
        if (context.getCurrentBaseState() instanceof StealableToTripleBase) {
            applySteal(
                    context,
                    Base.SECOND,
                    Base.THIRD,
                    context.getRunners().runnerAt(Base.SECOND).stealToTriple(),
                    statisticsRecorder);
        }
    }

    private void applySteal(
            GameBattingContext context,
            Base currentBase,
            Base targetBaseOfSteal,
            StealResult stealResult,
            GameStatisticsRecorder statisticsRecorder) {
        Runnable applyStealResult =
                switch (stealResult) {
                    case NOT_TRY -> () -> {};
                    case FAILURE ->
                            () -> {
                                context.setRunnerTo(currentBase, null);
                                context.addOutCounts(1);
                            };
                    case SUCCESS ->
                            () -> {
                                statisticsRecorder.recordSteal();
                                context.setRunnerTo(
                                        targetBaseOfSteal,
                                        context.getRunners().runnerAt(currentBase));
                                context.setRunnerTo(currentBase, null);
                            };
                };
        applyStealResult.run();
    }
}
