package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
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

        if (applyBuntResult(
                context,
                batter.bunt(context.getOutCount(), context.getCurrentBaseState()),
                statisticsRecorder)) {
            return;
        }

        BattingResult battingResult = batter.swing();
        if (battingResult == BattingResult.HIT_HOMER) {
            statisticsRecorder.recordHomeRun(context.getCurrentBaseState().runnerCount());
        }
        BasesState nextBaseState =
                switch (battingResult) {
                    case OUT -> {
                        context.getCurrentBaseState().out(context);
                        yield context.getCurrentBaseState();
                    }
                    case HIT_SINGLE ->
                            context.getCurrentBaseState().hitSingle(context, batter);
                    case HIT_DOUBLE ->
                            context.getCurrentBaseState().hitDouble(context, batter);
                    case HIT_TRIPLE ->
                            context.getCurrentBaseState().hitTriple(context, batter);
                    case HIT_HOMER -> context.getCurrentBaseState().hitHomer(context, batter);
                };
        context.replaceBaseState(nextBaseState);
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
                context.replaceBaseState(context.getCurrentBaseState().advance(Base.FIRST));
                context.addOutCounts(1);
                yield true;
            }
        };
    }

    private void trySteal(GameBattingContext context, GameStatisticsRecorder statisticsRecorder) {
        if (context.getCurrentBaseState() instanceof StealableToDoubleBase stealable) {
            applySteal(
                    context,
                    Base.FIRST,
                    Base.SECOND,
                    stealable.runnerOnFirst().stealToDouble(),
                    statisticsRecorder);
        }
        if (context.getCurrentBaseState() instanceof StealableToTripleBase stealable) {
            applySteal(
                    context,
                    Base.SECOND,
                    Base.THIRD,
                    stealable.runnerOnSecond().stealToTriple(),
                    statisticsRecorder);
        }
    }

    private void applySteal(
            GameBattingContext context,
            Base currentBase,
            Base targetBaseOfSteal,
            StealResult stealResult,
            GameStatisticsRecorder statisticsRecorder) {
        BasesState nextBaseState =
                switch (stealResult) {
                    case NOT_TRY -> context.getCurrentBaseState();
                    case FAILURE ->
                            failSteal(context, currentBase);
                    case SUCCESS ->
                            succeedSteal(context, currentBase, targetBaseOfSteal, statisticsRecorder);
                };
        context.replaceBaseState(nextBaseState);
    }

    private BasesState failSteal(GameBattingContext context, Base currentBase) {
        context.addOutCounts(1);
        return context.getCurrentBaseState().withRunnerAt(currentBase, null);
    }

    private BasesState succeedSteal(
            GameBattingContext context,
            Base currentBase,
            Base targetBase,
            GameStatisticsRecorder statisticsRecorder) {
        statisticsRecorder.recordSteal();
        BatterEntity runner = context.getCurrentBaseState().runnerAt(currentBase);
        return context.getCurrentBaseState().withRunnerAt(targetBase, runner).withRunnerAt(currentBase, null);
    }
}
