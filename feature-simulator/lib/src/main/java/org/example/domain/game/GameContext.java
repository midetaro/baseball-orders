package org.example.domain.game;

import org.example.domain.player.Player;
import org.example.domain.state.BasesState;
import org.example.domain.state.NoBasesState;

import java.util.List;

public class GameContext {

    private int inning = 1;
    private int totalScore = 0;
    private int outCounts = 0;
    private BasesState currentBaseState = new NoBasesState(); // 初期状態

    public void updateBaseState(BasesState state) {
        currentBaseState = state;
    }

    public void addScore(int runs) {
        totalScore += runs;
    }

    public void addOut(int diff) {
        outCounts += diff;

        if (outCounts >= 3) {
            this.goToNextInning();
        }
    }

    private void goToNextInning() {
        inning++;
        currentBaseState = new NoBasesState();
        outCounts = 0;
        if (inning == 9) {
            currentBaseState = new NoBasesState();
        }
    }

    public boolean isGameOver() {
        return inning == 9;
    }

    public GameResult simulate(List<Player> lineup) {
        GameContext ctx = new GameContext(lineup);
        while (!ctx.isGameOver()) {
            ctx.processAtBat(ctx.getCurrentBatter());
        }
        return ctx.toResult(); // 最終スコアだけを返す
    }

}
