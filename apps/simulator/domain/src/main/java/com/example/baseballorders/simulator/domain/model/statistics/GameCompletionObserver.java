package com.example.baseballorders.simulator.domain.model.statistics;

/** Observes the completion of a simulated game. */
@FunctionalInterface
public interface GameCompletionObserver {

    /**
     * Receives a completed game's final score and play statistics.
     *
     * @param totalScore final score of the completed game
     * @param gameStatistics play statistics from the completed game
     */
    void onGameCompleted(long totalScore, GameStatistics gameStatistics);
}
