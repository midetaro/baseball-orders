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

    public GameBattingContext(LineUpEntity batterEntityOrders) {
        this.batterEntityOrders = batterEntityOrders.getBatterEntities();
        this.numberOfNextBatter = 0;
    }

    public void updateBaseState(BasesState state) {
        currentBaseState = state;
    }

    public void setRunnerTo(Base base, Optional<BatterEntity> batter) {
        Runnable setRunner =
                switch (base) {
                    case FIRST -> () -> runnerOnFirstBase = batter;
                    case SECOND -> () -> runnerOnSecondBase = batter;
                    case THIRD -> () -> runnerOnThirdBase = batter;
                };
        setRunner.run();
    }

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

    public void addScore(long runs) {
        System.out.println("得点を追加します: " + runs);
        totalScore += runs;
    }

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

    public void cleanAllBases() {
        this.moveRunnerNthBase(Base.THIRD);
    }
}
