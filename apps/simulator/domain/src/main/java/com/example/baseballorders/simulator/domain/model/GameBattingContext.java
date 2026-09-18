package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.NoBasesState;
import com.example.baseballorders.simulator.domain.model.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.model.statistics.GameStatisticsRecorder;
import java.util.List;
import lombok.Getter;

public class GameBattingContext {

    private long inning = 1;
    @Getter private long totalScore = 0;
    @Getter private OutCount outCount = OutCount.NO_OUT;
    @Getter private BasesState currentBaseState = new NoBasesState();
    @Getter private BaseRunners runners = BaseRunners.empty();

    private final List<BatterEntity> batterEntityOrders;
    private final AtBatProcessor atBatProcessor = new AtBatProcessor();
    private final GameStatisticsRecorder statisticsRecorder = new GameStatisticsRecorder();

    private int numberOfNextBatter;
    @Getter private boolean isGameOver = false;

    /**
     * 指定された打順で、初回・無死・走者なし・無得点の試合状態を作成する。
     *
     * @param batterEntityOrders 試合で使用する打順
     */
    public GameBattingContext(LineUpEntity batterEntityOrders) {
        this.batterEntityOrders = batterEntityOrders.getBatterEntities();
        this.numberOfNextBatter = 0;
    }

    /**
     * 指定した塁の走者を設定する。塁状態の再判定は行わない。
     *
     * @param base 走者を設定する塁
     * @param batter 設定する走者。{@code null} の場合はその塁の走者を取り除く
     */
    public void setRunnerTo(Base base, BatterEntity batter) {
        runners.setRunner(base, batter);
    }

    /**
     * 全走者を指定した塁数だけ進め、三塁を越える走者を塁上から取り除く。得点加算と塁状態の再判定は行わない。
     *
     * @param nthBase 進める塁数（FIRST は一つ、SECOND は二つ、THIRD は三つ）
     */
    public void moveRunnerNthBase(Base nthBase) {
        runners = runners.advance(nthBase);
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
            updateBaseStateOf();
            outCount = OutCount.NO_OUT;
            isGameOver = true;
            return;
        }
        inning++;
        this.cleanAllBases();
        updateBaseStateOf();
        outCount = OutCount.NO_OUT;
    }

    /** 盗塁を試みた後に現在の打者の打撃結果を適用し、塁状態と次の打者の位置を更新する。 */
    public void nextAtBat() {
        var batter = batterEntityOrders.get(numberOfNextBatter);
        atBatProcessor.process(this, batter, statisticsRecorder);
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

    /** 各塁の走者の有無から、打撃結果の適用に使用する塁状態を再判定する。 */
    public void updateBaseStateOf() {
        this.currentBaseState = BasesStateResolver.resolve(runners);
    }

    /** 全ての塁から走者を取り除く。得点加算と塁状態の再判定は行わない。 */
    public void cleanAllBases() {
        this.moveRunnerNthBase(Base.THIRD);
    }
}
