package org.example.domain.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.code.BattingResult;
import org.example.domain.model.player.Batter;
import org.example.domain.model.state.BasesState;
import org.example.domain.model.state.NoBasesState;

import java.util.List;

@Slf4j
@Getter
public class GameContext {

    private int inning = 1;
    private int totalScore = 0;
    private int outCounts = 0;
    private BasesState currentBaseState = new NoBasesState(); // 初期状態
    private final List<Batter> batters;
    private int numberOfNextBatter;
    private boolean isGameOver = false;

    public GameContext(List<Batter> batters) {
        this.batters = batters;
        this.numberOfNextBatter = 0;
    }

    public void updateBaseState(BasesState state) {
        currentBaseState = state;
    }

    public void addScore(int runs) {
        totalScore += runs;
    }

    public void addOutCounts(int diff) {
        outCounts += diff;
        if (outCounts >= 3) {
            this.goToNextInning();
        }
    }

    private void goToNextInning() {
        if (inning == 9) {
            currentBaseState = new NoBasesState();
            outCounts = 0;
            isGameOver = true;
            return;
        }
        inning++;
        log.info("{}:回に移動します", inning);
        currentBaseState = new NoBasesState();
        outCounts = 0;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void nextAtBat() {
        var batter = batters.get(numberOfNextBatter);
        BattingResult battingResult = batter.swing();
        log.info("BattingResult: {}", battingResult);
        switch (battingResult) {
            case OUT -> currentBaseState.out(this, batter);
            case HIT_SINGLE -> currentBaseState.singleHit(this, batter);
            case HIT_DOUBLE -> currentBaseState.hitDouble(this, batter);
            case HIT_TRIPLE -> currentBaseState.hitTriple(this, batter);
            case HIT_HOMER -> currentBaseState.hitHomer(this, batter);
        }
        this.toNextBatter();
    }

    private void toNextBatter() {
        if (this.numberOfNextBatter == 8) {
            this.numberOfNextBatter = 0;
        }
        this.numberOfNextBatter++;
    }
}
