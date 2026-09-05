package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.model.state.*;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class GameBattingContext {

    private long inning = 1;
    private long totalScore = 0;
    private OutCount outCount = OutCount.NO_OUT;
    private BasesState currentBaseState = new NoBasesState();
    @Setter private Optional<BatterEntity> runnerOnFirstBase = Optional.empty();
    @Setter private Optional<BatterEntity> runnerOnSecondBase = Optional.empty();
    @Setter private Optional<BatterEntity> runnerOnThirdBase = Optional.empty();

    private final List<BatterEntity> batterEntityOrders;
    private int numberOfNextBatter;
    private boolean isGameOver = false;

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
     * 打撃結果の適用に使用する塁状態を設定する。各塁の走者は変更しない。
     *
     * @param state 設定する塁状態
     */
    public void updateBaseState(BasesState state) {
        currentBaseState = state;
    }

    /**
     * 指定した塁の走者を設定する。塁状態の再判定は行わない。
     *
     * @param base 走者を設定する塁
     * @param batter 設定する走者。空の場合はその塁の走者を取り除く
     */
    public void setRunnerTo(Base base, Optional<BatterEntity> batter) {
        Runnable setRunner =
                switch (base) {
                    case FIRST -> () -> runnerOnFirstBase = batter;
                    case SECOND -> () -> runnerOnSecondBase = batter;
                    case THIRD -> () -> runnerOnThirdBase = batter;
                };
        setRunner.run();
    }

    /**
     * 全走者を指定した塁数だけ進め、三塁を越える走者を塁上から取り除く。得点加算と塁状態の再判定は行わない。
     *
     * @param nthBase 進める塁数（FIRST は一つ、SECOND は二つ、THIRD は三つ）
     */
    public void moveRunnerNthBase(Base nthBase) {
        Runnable moveRunners =
                switch (nthBase) {
                    case FIRST ->
                            () -> {
                                runnerOnThirdBase = runnerOnSecondBase;
                                runnerOnSecondBase = runnerOnFirstBase;
                                runnerOnFirstBase = Optional.empty();
                            };
                    case SECOND ->
                            () -> {
                                runnerOnThirdBase = runnerOnFirstBase;
                                runnerOnSecondBase = Optional.empty();
                                runnerOnFirstBase = Optional.empty();
                            };
                    case THIRD ->
                            () -> {
                                runnerOnThirdBase = Optional.empty();
                                runnerOnFirstBase = Optional.empty();
                                runnerOnSecondBase = Optional.empty();
                            };
                };
        moveRunners.run();
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
        // --- Steal ---
        this.trySteal();
        this.updateBaseStateOf();

        // --- 打撃 ---
        BattingResult battingResult = batter.swing();
        Runnable applyBattingResult =
                switch (battingResult) {
                    case OUT -> () -> currentBaseState.out(this);
                    case HIT_SINGLE -> () -> currentBaseState.hitSingle(this, batter);
                    case HIT_DOUBLE -> () -> currentBaseState.hitDouble(this, batter);
                    case HIT_TRIPLE -> () -> currentBaseState.hitTriple(this, batter);
                    case HIT_HOMER -> () -> currentBaseState.hitHomer(this, batter);
                };
        applyBattingResult.run();
        this.updateBaseStateOf();
        this.toNextBatter();
    }

    private void trySteal() {
        if (currentBaseState instanceof StealableToDoubleBase) {
            stealToDouble();
        }
        if (currentBaseState instanceof StealableToTripleBase) {
            stealToTriple();
        }
    }

    private void stealToDouble() {
        applySteal(Base.FIRST, Base.SECOND, getRunnerIndexOf(Base.FIRST).get().stealToDouble());
    }

    private void stealToTriple() {
        applySteal(Base.SECOND, Base.THIRD, getRunnerIndexOf(Base.SECOND).get().stealToTriple());
    }

    private void applySteal(Base currentBase, Base targetBaseOfSteal, StealResult stealResult) {
        Runnable applyStealResult =
                switch (stealResult) {
                    case NOT_TRY -> () -> {};
                    case FAILURE ->
                            () -> {
                                this.setRunnerTo(currentBase, Optional.empty());
                                this.addOutCounts(1);
                            };
                    case SUCCESS ->
                            () -> {
                                this.setRunnerTo(targetBaseOfSteal, getRunnerIndexOf(currentBase));
                                this.setRunnerTo(currentBase, Optional.empty());
                            };
                };
        applyStealResult.run();
    }

    private Optional<BatterEntity> getRunnerIndexOf(Base base) {
        return switch (base) {
            case FIRST -> runnerOnFirstBase;
            case SECOND -> runnerOnSecondBase;
            case THIRD -> runnerOnThirdBase;
        };
    }

    private void toNextBatter() {
        if (this.numberOfNextBatter == 8) {
            this.numberOfNextBatter = 0;
        }
        this.numberOfNextBatter++;
    }

    /** 各塁の走者の有無から、打撃結果の適用に使用する塁状態を再判定する。 */
    public void updateBaseStateOf() {

        if (runnerOnFirstBase.isPresent()
                && runnerOnSecondBase.isPresent()
                && runnerOnThirdBase.isPresent()) {
            this.currentBaseState = new FullBasesState();
        } else if (runnerOnFirstBase.isPresent() && runnerOnSecondBase.isPresent()) {
            this.currentBaseState = new FirstDoubleBaseState();
        } else if (runnerOnFirstBase.isPresent() && runnerOnThirdBase.isPresent()) {
            this.currentBaseState = new FirstThirdBaseState();
        } else if (runnerOnFirstBase.isPresent()) {
            this.currentBaseState = new SingleBasesState();
        } else if (runnerOnSecondBase.isPresent() && runnerOnThirdBase.isPresent()) {
            this.currentBaseState = new DoubleThirdBaseState();
        } else if (runnerOnSecondBase.isPresent()) {
            this.currentBaseState = new DoubleBaseState();
        } else if (runnerOnThirdBase.isPresent()) {
            this.currentBaseState = new ThirdBaseState();
        } else {
            this.currentBaseState = new NoBasesState();
        }
    }

    /** 全ての塁から走者を取り除く。得点加算と塁状態の再判定は行わない。 */
    public void cleanAllBases() {
        this.moveRunnerNthBase(Base.THIRD);
    }
}
