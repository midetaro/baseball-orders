package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.game.capability.Buntable;
import com.example.baseballorders.simulator.domain.game.capability.Stealable;
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
    private final NoBasesState noBasesState;
    private final SingleBasesState singleBasesState;
    private final DoubleBaseState doubleBaseState;
    private final FirstDoubleBaseState firstDoubleBaseState;
    private final ThirdBaseState thirdBaseState;
    private final FirstThirdBaseState firstThirdBaseState;
    private final DoubleThirdBaseState doubleThirdBaseState;
    private final FullBasesState fullBasesState;
    private final GameStatisticsRecorder statisticsRecorder = new GameStatisticsRecorder();
    private final GameCompletionObserver gameCompletionObserver;
    private final List<BatterEntity> batterEntityOrders;
    private final AtBatProcessor atBatProcessor = new AtBatProcessor();
    @Getter private long inning = 1;
    @Getter private long totalScore;
    @Getter private BasesState currentState;
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
        InningState inningState = new InningState();
        noBasesState = baseStateFactory.createNoBasesState(this, inningState);
        singleBasesState = baseStateFactory.createSingleBasesState(this, inningState);
        doubleBaseState = baseStateFactory.createDoubleBaseState(this, inningState);
        firstDoubleBaseState = baseStateFactory.createFirstDoubleBaseState(this, inningState);
        thirdBaseState = baseStateFactory.createThirdBaseState(this, inningState);
        firstThirdBaseState = baseStateFactory.createFirstThirdBaseState(this, inningState);
        doubleThirdBaseState = baseStateFactory.createDoubleThirdBaseState(this, inningState);
        fullBasesState = baseStateFactory.createFullBasesState(this, inningState);
        currentState = noBasesState;
    }

    /**
     * Stateが算出した得点を試合に加算する。
     *
     * @param runs 加算得点
     */
    public void addScore(long runs) {
        totalScore += runs;
    }

    /**
     * Stateから走者配置に一致する、この試合のStateへ切り替える。
     *
     * @param configuration 一塁・二塁・三塁の在塁を下位3ビットで表した値
     * @throws IllegalArgumentException 0から7以外の配置を指定した場合
     */
    public void changeState(int configuration) {
        currentState =
                switch (configuration) {
                    case 0 -> noBasesState;
                    case 1 -> singleBasesState;
                    case 2 -> doubleBaseState;
                    case 3 -> firstDoubleBaseState;
                    case 4 -> thirdBaseState;
                    case 5 -> firstThirdBaseState;
                    case 6 -> doubleThirdBaseState;
                    case 7 -> fullBasesState;
                    default -> throw new IllegalArgumentException("不正な走者配置: " + configuration);
                };
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
        if (atBatProcessor.process(this, batterEntityOrders.get(numberOfNextBatter))) {
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

    /** アウトを適用する。試合終了後は何もしない。 */
    public void out() {
        if (!isGameOver) {
            currentState.out();
        }
    }

    /** 打撃による凡退を適用し、三死目でなければ一定確率で先頭走者だけを進める。 */
    public void battingOut() {
        if (!isGameOver) {
            currentState.battingOut();
        }
    }

    /**
     * 四球を適用する。試合終了後は何もしない。
     *
     * @param batter 四球の打者
     */
    public void walk(BatterEntity batter) {
        if (!isGameOver) {
            currentState.walk(batter);
        }
    }

    /**
     * 単打を適用する。試合終了後は何もしない。
     *
     * @param batter 打撃した打者
     */
    public void hitSingle(BatterEntity batter) {
        if (!isGameOver) {
            currentState.hitSingle(batter);
        }
    }

    /**
     * 二塁打を適用する。試合終了後は何もしない。
     *
     * @param batter 打撃した打者
     */
    public void hitDouble(BatterEntity batter) {
        if (!isGameOver) {
            currentState.hitDouble(batter);
        }
    }

    /**
     * 三塁打を適用する。試合終了後は何もしない。
     *
     * @param batter 打撃した打者
     */
    public void hitTriple(BatterEntity batter) {
        if (!isGameOver) {
            currentState.hitTriple(batter);
        }
    }

    /** 本塁打を適用する。試合終了後は何もしない。 */
    public void hitHomer() {
        if (!isGameOver) {
            currentState.hitHomer();
        }
    }

    /** バント見送りを適用する。試合終了後は何もしない。 */
    public void buntNotTry() {
        if (!isGameOver && currentState instanceof Buntable buntable) {
            buntable.buntNotTry();
        }
    }

    /**
     * バント失敗を適用する。試合終了後は何もしない。
     *
     * @throws IllegalStateException 対応するプレー機会がない場合
     */
    public void buntFailure() {
        if (!isGameOver) {
            requireBuntable().buntFailure();
        }
    }

    /**
     * バント成功を適用する。試合終了後は何もしない。
     *
     * @throws IllegalStateException 対応するプレー機会がない場合
     */
    public void buntSuccess() {
        if (!isGameOver) {
            requireBuntable().buntSuccess();
        }
    }

    /** 盗塁見送りを適用する。試合終了後は何もしない。 */
    public void stealNotTry() {
        if (!isGameOver && currentState instanceof Stealable stealable) {
            stealable.stealNotTry();
        }
    }

    /**
     * 盗塁失敗を適用する。試合終了後は何もしない。
     *
     * @throws IllegalStateException 対応するプレー機会がない場合
     */
    public void stealFailure() {
        if (!isGameOver) {
            requireStealable().stealFailure();
        }
    }

    /**
     * 盗塁成功を適用する。試合終了後は何もしない。
     *
     * @throws IllegalStateException 対応するプレー機会がない場合
     */
    public void stealSuccess() {
        if (!isGameOver) {
            requireStealable().stealSuccess();
        }
    }

    private Buntable requireBuntable() {
        if (currentState instanceof Buntable buntable) {
            return buntable;
        }
        throw new IllegalStateException("犠打機会がありません");
    }

    private Stealable requireStealable() {
        if (currentState instanceof Stealable stealable) {
            return stealable;
        }
        throw new IllegalStateException("盗塁機会がありません");
    }
}
