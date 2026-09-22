package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.statistics.GameCompletionObserver;
import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsRecorder;
import java.util.List;
import lombok.Getter;

/** 試合全体の情報を保持し、プレーを現在の塁Stateへ委譲するContext。 */
public class GameBattingContext {
    private static final GameCompletionObserver NO_OPERATION_OBSERVER = (score, statistics) -> {};
    private final InningStateContext inningStateContext;
    private final GameStatisticsRecorder statisticsRecorder = new GameStatisticsRecorder();
    private final GameCompletionObserver gameCompletionObserver;
    private final List<BatterEntity> batterEntityOrders;
    private final AtBatProcessor atBatProcessor = new AtBatProcessor();
    @Getter private long inning = 1;
    private long totalScore;
    private int numberOfNextBatter;
    @Getter private boolean isGameOver;

    /**
     * 初回・無死・走者なしの試合を作成する。
     *
     * @param batterEntityOrders 試合で使用する打順
     */
    public GameBattingContext(LineUpEntity batterEntityOrders) {
        this(batterEntityOrders, NO_OPERATION_OBSERVER, new BaseStateFactory());
    }

    /**
     * 終了通知先を指定して試合を作成する。
     *
     * @param batterEntityOrders 試合で使用する打順
     * @param gameCompletionObserver 試合終了時の通知先
     */
    public GameBattingContext(
            LineUpEntity batterEntityOrders, GameCompletionObserver gameCompletionObserver) {
        this(batterEntityOrders, gameCompletionObserver, new BaseStateFactory());
    }

    /**
     * 同一イニング状態を共有する8種類のStateと試合を作成する。
     *
     * @param batterEntityOrders 試合で使用する打順
     * @param gameCompletionObserver 試合終了時の通知先
     * @param baseStateFactory 試合固有のStateを生成するファクトリ
     */
    public GameBattingContext(
            LineUpEntity batterEntityOrders,
            GameCompletionObserver gameCompletionObserver,
            BaseStateFactory baseStateFactory) {
        this.batterEntityOrders =
                batterEntityOrders.getBatterEntities().stream()
                        .map(batter -> batter.observedBy(statisticsRecorder))
                        .toList();
        this.gameCompletionObserver = gameCompletionObserver;
        inningStateContext = new InningStateContext(baseStateFactory, this::reflectCompletedInning);
    }

    InningStateContext inningStateContext() {
        return inningStateContext;
    }

    /** イニング終了を試合全体の得点・回数・完了通知へ反映する。 */
    private void reflectCompletedInning(long completedInning, long inningScore) {
        totalScore += inningScore;
        inning = completedInning == 9 ? completedInning : completedInning + 1;
        if (completedInning == 9) {
            isGameOver = true;
            gameCompletionObserver.onGameCompleted(totalScore, statisticsRecorder.snapshot());
        }
    }

    /**
     * 現在のイニングを終了し、得点と回数を試合へ反映する。
     *
     * <p>塁Stateからの終了処理をテストできるよう、集約内に公開する。
     */
    void completeInning() {
        if (isGameOver) {
            return;
        }
        inningStateContext.completeInning();
    }

    /** 盗塁・バント・打撃を処理し、打席が完了した場合だけ打順を進める。 */
    public void nextAtBat() {
        if (isGameOver) {
            return;
        }
        if (atBatProcessor.process(
                inningStateContext, batterEntityOrders.get(numberOfNextBatter))) {
            if (numberOfNextBatter == 8) {
                numberOfNextBatter = 0;
            }
            numberOfNextBatter++;
        }
    }

    /** 試合中に累積した打撃統計を返す。 @return 現時点の打撃統計 */
    public GameStatistics getGameStatistics() {
        return statisticsRecorder.snapshot();
    }

    /**
     * 終了済みイニングの合計と現在のイニングで発生した得点を返す。
     *
     * @return 現時点の試合総得点
     */
    public long getTotalScore() {
        return totalScore + inningStateContext.score();
    }
}
