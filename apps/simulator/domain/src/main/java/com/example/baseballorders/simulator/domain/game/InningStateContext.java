package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.BatterEntity;

/** 一試合のイニング状態と塁配置ごとのStateを所有し、プレーを現在のStateへ送る。 */
public final class InningStateContext {
    private final InningState inningState = new InningState();
    private final InningCompletionListener inningCompletionListener;
    private final NoBasesState noBasesState;
    private final SingleBasesState singleBasesState;
    private final DoubleBaseState doubleBaseState;
    private final FirstDoubleBaseState firstDoubleBaseState;
    private final ThirdBaseState thirdBaseState;
    private final FirstThirdBaseState firstThirdBaseState;
    private final DoubleThirdBaseState doubleThirdBaseState;
    private final FullBasesState fullBasesState;
    private BasesState currentBaseState;
    private long inning = 1;
    private long score;
    private boolean gameOver;

    InningStateContext(
            BaseStateFactory factory, InningCompletionListener inningCompletionListener) {
        this.inningCompletionListener = inningCompletionListener;
        noBasesState = factory.createNoBasesState(this);
        singleBasesState = factory.createSingleBasesState(this);
        doubleBaseState = factory.createDoubleBaseState(this);
        firstDoubleBaseState = factory.createFirstDoubleBaseState(this);
        thirdBaseState = factory.createThirdBaseState(this);
        firstThirdBaseState = factory.createFirstThirdBaseState(this);
        doubleThirdBaseState = factory.createDoubleThirdBaseState(this);
        fullBasesState = factory.createFullBasesState(this);
        currentBaseState = noBasesState;
    }

    BasesState currentBaseState() {
        return currentBaseState;
    }

    BatterEntity runnerAt(Base base) {
        return inningState.runnerAt(base);
    }

    OutCount outCount() {
        return inningState.getOutCount();
    }

    void addOut() {
        inningState.addOut();
    }

    void reset() {
        inningState.reset();
    }

    void place(BatterEntity first, BatterEntity second, BatterEntity third) {
        inningState.place(first, second, third);
    }

    long inning() {
        return inning;
    }

    boolean isGameOver() {
        return gameOver;
    }

    void addScore(long runs) {
        score += runs;
    }

    void completeInning() {
        if (gameOver) {
            return;
        }
        inningCompletionListener.onInningCompleted(inning, score);
        score = 0;
        if (inning == 9) {
            gameOver = true;
        } else {
            inning++;
        }
    }

    long score() {
        return score;
    }

    void changeState(int configuration) {
        currentBaseState =
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
}

@FunctionalInterface
interface InningCompletionListener {
    void onInningCompleted(long completedInning, long inningScore);
}
