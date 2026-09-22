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
    @Getter private long totalScore;
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
        inningStateContext = new InningStateContext(this, baseStateFactory);
    }

    /**
     * Stateが算出した得点を試合に加算する。
     *
     * @param runs 加算得点
     */
    public void addScore(long runs) {
        totalScore += runs;
    }

    InningStateContext inningStateContext() {
        return inningStateContext;
    }

    /** Stateによる三死の初期化後に次の回へ進め、九回終了なら一度だけ結果を通知する。 */
    public void completeInning() {
        if (isGameOver) {
            return;
        }
        if (inning == 9) {
            isGameOver = true;
            gameCompletionObserver.onGameCompleted(totalScore, statisticsRecorder.snapshot());
        } else {
            inning++;
        }
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
}
