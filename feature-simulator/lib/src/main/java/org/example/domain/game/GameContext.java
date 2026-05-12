package org.example.domain.game;

import org.example.domain.player.Batter;
import org.example.domain.state.BasesState;
import org.example.domain.state.NoBasesState;

public class GameContext {

    private int inning = 1;
    private int totalScore = 0;
    private int outs = 0;
    private final boolean[] bases = new boolean[3]; // [1塁, 2塁, 3塁]
    private BasesState currentState = new NoBasesState(); // 初期状態

    // 状態を切り替えるメソッド
    public void updateState(BasesState state) {
        currentState = state;
    }

    // スコアを加算するメソッド
    public void addScore(int runs) {
        totalScore += runs;
    }

    public void addOut(int diff) {
        outs += diff;
        if (outs == 3) {
            this.refreshInning();
        }
    }

    private void refreshInning() {
        inning++;
        currentState = new NoBasesState();
        this.cleanAllBases();

    }

    private void cleanAllBases() {
        updateBases(false, false, false);
    }

    private void updateBases(boolean first, boolean second, boolean third) {
        bases[0] = first;
        bases[1] = second;
        bases[2] = third;
    }

    // 1打席実行
    public void processAtBat(Batter batter) {
        currentState.singleHit(this, batter);
    }
}
