package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.play.StealTarget;
import com.example.baseballorders.simulator.domain.statistics.GameTransitionBuilder;
import com.example.baseballorders.simulator.domain.statistics.GameTransitionRecorder;
import com.example.baseballorders.simulator.domain.statistics.PlayResultObserver;
import java.util.function.LongSupplier;

/**
 * 1試合実行のためのプレー結果Observer。
 *
 * <p>プレー結果を委譲先（試合統計の購読者）へそのまま転送しつつ、{@code domain.game}パッケージが持つ
 * アクセス権限を使って、その時点のイニング・アウト数・累積得点・走者状況を状況推移として記録へ蓄積する。
 *
 * <p>{@link PlayResultObserver}の契約どおり、各コールバックはそのプレー自身の塁状態遷移が適用される直前に届く。
 * そのため、記録される状況推移のアウト数・得点・走者状況は「それまでの全プレー適用後」の状況を表す。
 */
final class SingleGameTransitionObserver implements PlayResultObserver {

    private final PlayResultObserver delegate;
    private final GameTransitionRecorder recorder;
    private final InningStateContext inningStateContext;
    private final LongSupplier inningSupplier;
    private final LongSupplier totalScoreSupplier;

    SingleGameTransitionObserver(
            PlayResultObserver delegate,
            GameTransitionRecorder recorder,
            InningStateContext inningStateContext,
            LongSupplier inningSupplier,
            LongSupplier totalScoreSupplier) {
        this.delegate = delegate;
        this.recorder = recorder;
        this.inningStateContext = inningStateContext;
        this.inningSupplier = inningSupplier;
        this.totalScoreSupplier = totalScoreSupplier;
    }

    @Override
    public void onBattingResult(BattingResult battingResult, int runnerCount) {
        delegate.onBattingResult(battingResult, runnerCount);
        recordTransition(describeBatting(battingResult));
    }

    @Override
    public void onBuntResult(BuntResult buntResult, BuntType buntType) {
        delegate.onBuntResult(buntResult, buntType);
        switch (buntResult) {
            case NOT_TRY -> {}
            case SUCCESS -> recordTransition(describeBunt(true, buntType));
            case FAILURE -> recordTransition(describeBunt(false, buntType));
        }
    }

    @Override
    public void onStealResult(StealResult stealResult, StealTarget stealTarget) {
        delegate.onStealResult(stealResult, stealTarget);
        switch (stealResult) {
            case NOT_TRY -> {}
            case SUCCESS -> recordTransition(describeSteal(true, stealTarget));
            case FAILURE -> recordTransition(describeSteal(false, stealTarget));
        }
    }

    private void recordTransition(String actionResult) {
        recorder.record(
                GameTransitionBuilder.gameTransition()
                        .inning(inningSupplier.getAsLong())
                        .actionResult(actionResult)
                        .outCount(outCountValue())
                        .cumulativeScore(totalScoreSupplier.getAsLong())
                        .runnerState(describeRunnerState())
                        .build());
    }

    private int outCountValue() {
        return switch (inningStateContext.outCount()) {
            case NO_OUT -> 0;
            case ONE_OUT -> 1;
            case TWO_OUT -> 2;
            case THREE_OUT -> 3;
        };
    }

    private String describeRunnerState() {
        boolean first = inningStateContext.runnerAt(Base.FIRST) != null;
        boolean second = inningStateContext.runnerAt(Base.SECOND) != null;
        boolean third = inningStateContext.runnerAt(Base.THIRD) != null;
        int configuration = (first ? 1 : 0) | (second ? 2 : 0) | (third ? 4 : 0);
        return switch (configuration) {
            case 0 -> "走者なし";
            case 1 -> "一塁";
            case 2 -> "二塁";
            case 3 -> "一・二塁";
            case 4 -> "三塁";
            case 5 -> "一・三塁";
            case 6 -> "二・三塁";
            case 7 -> "満塁";
            default -> throw new IllegalStateException("走者配置は0から7の範囲でなければならない: " + configuration);
        };
    }

    private static String describeBatting(BattingResult battingResult) {
        return switch (battingResult) {
            case STRIKEOUT -> "三振";
            case BATTED_OUT -> "凡打";
            case WALK -> "四球";
            case HIT_SINGLE -> "単打";
            case HIT_DOUBLE -> "二塁打";
            case HIT_TRIPLE -> "三塁打";
            case HIT_HOMER -> "本塁打";
        };
    }

    private static String describeBunt(boolean success, BuntType buntType) {
        String type =
                switch (buntType) {
                    case ADVANCING -> "バント";
                    case SQUEEZE -> "スクイズ";
                };
        return type + (success ? "成功" : "失敗");
    }

    private static String describeSteal(boolean success, StealTarget stealTarget) {
        String target =
                switch (stealTarget) {
                    case SECOND -> "二塁";
                    case THIRD -> "三塁";
                };
        return "盗塁" + (success ? "成功" : "失敗") + "(" + target + ")";
    }
}
