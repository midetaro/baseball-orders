package com.example.baseballorders.simulator.domain.statistics;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.StealResult;

/** Observes results produced by a batter during one game. */
public interface PlayResultObserver {

    /**
     * Receives a batting result before its base-state transition is applied.
     *
     * @param battingResult batting result
     * @param runnerCount runner count before the batting result
     */
    void onBattingResult(BattingResult battingResult, int runnerCount);

    /**
     * Receives a bunt result.
     *
     * @param buntResult bunt result
     */
    void onBuntResult(BuntResult buntResult);

    /**
     * Receives a steal result.
     *
     * @param stealResult steal result
     */
    void onStealResult(StealResult stealResult);
}
