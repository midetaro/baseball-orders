package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
public class GameContext {

    private long inning = 1;
    private long totalScore = 0;
    private long outCounts = 0;
    private BasesState currentBaseState = new NoBasesState();
    @Setter
    private Optional<BatterEntity> runnerOnFirstBase = Optional.empty();
    @Setter
    private Optional<BatterEntity> runnerOnSecondBase = Optional.empty();
    @Setter
    private Optional<BatterEntity> runnerOnThirdBase = Optional.empty();

    private final List<BatterEntity> batterEntityOrders;
    private int numberOfNextBatter;
    private boolean isGameOver = false;

    public GameContext(LineUpEntity batterEntityOrders) {
        this.batterEntityOrders = batterEntityOrders.getBatterEntities();
        this.numberOfNextBatter = 0;
    }

    public void updateBaseState(BasesState state) {
        currentBaseState = state;
    }

    public void setRunnerTo(int nameOfBase, Optional<BatterEntity> batter) {
        if (nameOfBase == 1) {
            runnerOnFirstBase = batter;
        } else if (nameOfBase == 2) {
            runnerOnSecondBase = batter;
        } else if (nameOfBase == 3) {
            runnerOnThirdBase = batter;
        }
    }

    public void moveRunnerNthBase(int nthBase) {
        if (nthBase == 1) {
            runnerOnThirdBase = runnerOnSecondBase;
            runnerOnSecondBase = runnerOnFirstBase;
            runnerOnFirstBase = Optional.empty();
        } else if (nthBase == 2) {
            runnerOnThirdBase = runnerOnFirstBase;
            runnerOnSecondBase = Optional.empty();
            runnerOnFirstBase = Optional.empty();
        } else if (nthBase == 3 || nthBase == 4) {
            runnerOnThirdBase = Optional.empty();
            runnerOnFirstBase = Optional.empty();
            runnerOnSecondBase = Optional.empty();
        }
    }

    public void addScore(long runs) {
        System.out.println("得点を追加します: " + runs);
        totalScore += runs;
    }

    public void addOutCounts(long diff) {
        outCounts += diff;
        if (outCounts >= 3) {
            this.goToNextInning();
        }
    }

    private void goToNextInning() {
        if (inning == 9) {
            this.cleanAllBases();
            updateBaseStateOf();
            outCounts = 0;
            isGameOver = true;
            return;
        }
        inning++;
        log.info("{}:回に移動します", inning);
        this.cleanAllBases();
        updateBaseStateOf();
        outCounts = 0;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void nextAtBat() {
        var batter = batterEntityOrders.get(numberOfNextBatter);
        // --- Steal ---
        this.trySteal();
        this.updateBaseStateOf();

        // --- 打撃 ---
        BattingResult battingResult = batter.swing();
        log.info("BattingResult: {}", battingResult);
        switch (battingResult) {
            case OUT -> currentBaseState.out(this);
            case HIT_SINGLE -> currentBaseState.hitSingle(this, batter);
            case HIT_DOUBLE -> currentBaseState.hitDouble(this, batter);
            case HIT_TRIPLE -> currentBaseState.hitTriple(this, batter);
            case HIT_HOMER -> currentBaseState.hitHomer(this, batter);
        }
        this.updateBaseStateOf();
        System.out.println("======= currentBaseState:" + currentBaseState);
        this.toNextBatter();
    }

    private void trySteal() {
        if (currentBaseState instanceof StealableToDoubleBase) {
            stealTo(2);
        }
        if (currentBaseState instanceof StealableToTripleBase) {
            stealTo(3);
        }
    }

    private void stealTo(int targetBaseOfSteal) {
        int currentBase = targetBaseOfSteal - 1;
        if (targetBaseOfSteal != 2 && targetBaseOfSteal != 3) {
            throw new IllegalArgumentException("targetBaseOfSteal > 3");
        }
        StealResult stealResult;
        if (targetBaseOfSteal == 2) {
            stealResult = getRunnerIndexOf(targetBaseOfSteal - 1).get().stealToDouble();
        } else {
            stealResult = getRunnerIndexOf(targetBaseOfSteal - 1).get().stealToTriple();
        }
        switch (stealResult) {
            case FAILURE -> {
                System.out.printf("[%s]塁への盗塁が失敗しました。%n", targetBaseOfSteal);
                this.setRunnerTo(currentBase, Optional.empty());
                this.addOutCounts(1);
            }
            case SUCCESS -> {
                System.out.printf("[%s]塁への盗塁が成功しました。%n", targetBaseOfSteal);
                this.setRunnerTo(targetBaseOfSteal, getRunnerIndexOf(currentBase));
                this.setRunnerTo(currentBase, Optional.empty());
            }
        }
    }


    private Optional<BatterEntity> getRunnerIndexOf(int nameOfBase) {
        if (nameOfBase == 1) {
            return runnerOnFirstBase;
        } else if (nameOfBase == 2) {
            return runnerOnSecondBase;
        } else if (nameOfBase == 3) {
            return runnerOnThirdBase;
        }
        return Optional.empty();
    }

    private void toNextBatter() {
        if (this.numberOfNextBatter == 8) {
            this.numberOfNextBatter = 0;
        }
        this.numberOfNextBatter++;
    }

    public void updateBaseStateOf() {

        if (runnerOnFirstBase.isPresent() && runnerOnSecondBase.isPresent() && runnerOnThirdBase.isPresent()) {
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

    public void cleanAllBases() {
        this.moveRunnerNthBase(3);
    }
}
