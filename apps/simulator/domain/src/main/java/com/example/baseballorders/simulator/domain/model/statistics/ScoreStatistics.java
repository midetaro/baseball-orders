package com.example.baseballorders.simulator.domain.model.statistics;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** Aggregated score statistics for a completed group of simulated games. */
@Builder(style = BuilderStyle.STAGED)
public record ScoreStatistics(
        double averageScore,
        double medianScore,
        int maximumScore,
        int homeRunCount,
        int soloHomeRunCount,
        int twoRunHomeRunCount,
        int threeRunHomeRunCount,
        int grandSlamCount,
        int buntCount,
        int stealCount) {

    /**
     * Creates score-only statistics with no recorded batting events.
     *
     * @param averageScore average score
     * @param medianScore median score
     * @param maximumScore maximum score
     */
    public ScoreStatistics(double averageScore, double medianScore, int maximumScore) {
        this(averageScore, medianScore, maximumScore, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Adds the supplied per-game batting-event counts to these score statistics.
     *
     * @param gameStatistics statistics from each simulated game
     * @return score statistics including the aggregated batting events
     */
    public ScoreStatistics withGameStatistics(java.util.List<GameStatistics> gameStatistics) {
        return new ScoreStatistics(
                averageScore,
                medianScore,
                maximumScore,
                gameStatistics.stream().mapToInt(GameStatistics::homeRunCount).sum(),
                gameStatistics.stream().mapToInt(GameStatistics::soloHomeRunCount).sum(),
                gameStatistics.stream().mapToInt(GameStatistics::twoRunHomeRunCount).sum(),
                gameStatistics.stream().mapToInt(GameStatistics::threeRunHomeRunCount).sum(),
                gameStatistics.stream().mapToInt(GameStatistics::grandSlamCount).sum(),
                gameStatistics.stream().mapToInt(GameStatistics::buntCount).sum(),
                gameStatistics.stream().mapToInt(GameStatistics::stealCount).sum());
    }
}
