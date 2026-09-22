package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import java.util.HashMap;
import java.util.Map;

/** 一試合のイニング状態と塁配置ごとのStateを所有し、プレーを現在のStateへ送る。 */
public final class InningStateContext {
    private final InningState inningState = new InningState();
    private final InningCompletionListener inningCompletionListener;
    private final BaseStateFactory baseStateFactory;
    private final Map<Integer, BasesState> baseStates = new HashMap<>();
    private BasesState currentBaseState;
    private long inning = 1;
    private long score;
    private boolean gameOver;

    InningStateContext(
            BaseStateFactory factory, InningCompletionListener inningCompletionListener) {
        this.inningCompletionListener = inningCompletionListener;
        baseStateFactory = factory;
        changeState(0);
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
                baseStates.computeIfAbsent(
                        configuration,
                        stateConfiguration -> baseStateFactory.create(this, stateConfiguration));
    }
}

@FunctionalInterface
interface InningCompletionListener {
    void onInningCompleted(long completedInning, long inningScore);
}
