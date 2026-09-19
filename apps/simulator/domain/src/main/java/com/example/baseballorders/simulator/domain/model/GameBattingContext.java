package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.statistics.GameCompletionObserver;
import com.example.baseballorders.simulator.domain.model.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.model.statistics.GameStatisticsRecorder;
import java.util.List;
import lombok.Getter;

public class GameBattingContext {

    private static final GameCompletionObserver NO_OPERATION_OBSERVER =
            (totalScore, gameStatistics) -> {};

    private long inning = 1;
    @Getter private long totalScore = 0;
    @Getter private OutCount outCount = OutCount.NO_OUT;
    @Getter private BasesState currentBaseState = BasesState.empty();

    private final GameStatisticsRecorder statisticsRecorder = new GameStatisticsRecorder();
    private final GameCompletionObserver gameCompletionObserver;
    private final List<BatterEntity> batterEntityOrders;
    private final AtBatProcessor atBatProcessor = new AtBatProcessor();

    private int numberOfNextBatter;
    @Getter private boolean isGameOver = false;

    /**
     * 指定された打順で、初回・無死・走者なし・無得点の試合状態を作成する。
     *
     * @param batterEntityOrders 試合で使用する打順
     */
    public GameBattingContext(LineUpEntity batterEntityOrders) {
        this(batterEntityOrders, NO_OPERATION_OBSERVER);
    }

    /**
     * 指定された打順と試合終了時の通知先で、初回・無死・走者なし・無得点の試合状態を作成する。
     *
     * @param batterEntityOrders 試合で使用する打順
     * @param gameCompletionObserver 試合終了時の通知先
     */
    public GameBattingContext(
            LineUpEntity batterEntityOrders, GameCompletionObserver gameCompletionObserver) {
        this.batterEntityOrders =
                batterEntityOrders.getBatterEntities().stream()
                        .map(batter -> batter.observedBy(statisticsRecorder))
                        .toList();
        this.gameCompletionObserver = gameCompletionObserver;
        this.numberOfNextBatter = 0;
    }

    /**
     * 総得点に指定した得点を加算し、加算内容を標準出力に表示する。
     *
     * @param runs 加算する得点
     */
    public void addScore(long runs) {
        totalScore += runs;
    }

    /**
     * アウト数を加算する。三死になると走者とアウト数をリセットし、次の回へ進むか、九回なら試合終了にする。
     *
     * @param diff 加算するアウト数（0 以上）
     */
    public void addOutCounts(long diff) {
        if (isGameOver) {
            return;
        }
        outCount = outCount.add(diff);
        boolean inningOver =
                switch (outCount) {
                    case NO_OUT, ONE_OUT, TWO_OUT -> false;
                    case THREE_OUT -> true;
                };
        if (inningOver) {
            this.goToNextInning();
        }
    }

    private void goToNextInning() {
        if (inning == 9) {
            this.cleanAllBases();
            outCount = OutCount.NO_OUT;
            isGameOver = true;
            gameCompletionObserver.onGameCompleted(totalScore, statisticsRecorder.snapshot());
            return;
        }
        inning++;
        this.cleanAllBases();
        outCount = OutCount.NO_OUT;
    }

    /** 盗塁を試みた後に現在の打者の打撃結果を適用し、塁状態と次の打者の位置を更新する。 */
    public void nextAtBat() {
        var batter = batterEntityOrders.get(numberOfNextBatter);
        atBatProcessor.process(this, batter);
        this.toNextBatter();
    }

    /**
     * Returns the batting statistics accumulated in this game.
     *
     * @return completed-game statistics at the time of this call
     */
    public GameStatistics getGameStatistics() {
        return statisticsRecorder.snapshot();
    }

    private void toNextBatter() {
        if (this.numberOfNextBatter == 8) {
            this.numberOfNextBatter = 0;
        }
        this.numberOfNextBatter++;
    }

    void replaceBaseState(BasesState baseState) {
        this.currentBaseState = baseState;
    }

    private void cleanAllBases() {
        this.currentBaseState = BasesState.empty();
    }
}
