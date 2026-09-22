package com.example.baseballorders.simulator.domain.statistics;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.play.StealTarget;

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
     * @param buntType tactical bunt type
     */
    void onBuntResult(BuntResult buntResult, BuntType buntType);

    /**
     * Receives a steal result.
     *
     * @param stealResult steal result
     * @param stealTarget destination base
     */
    void onStealResult(StealResult stealResult, StealTarget stealTarget);
}
