package com.example.baseballorders.simulator.domain.statistics;

import java.util.*;

/** Accumulates completed-game scores and play statistics. */
public final class ScoreAccumulator implements GameCompletionObserver {
    private final List<Integer> scores = new ArrayList<>();
    private final Map<Integer, Integer> scoreDistribution = new TreeMap<>();
    private long totalScore;
    private int hitCount;
    private int singleHitCount;
    private int doubleHitCount;
    private int tripleHitCount;
    private int homeRunCount;
    private int soloHomeRunCount;
    private int twoRunHomeRunCount;
    private int threeRunHomeRunCount;
    private int grandSlamCount;
    private int buntCount;
    private int stealCount;
    private int buntFailureCount;
    private int stealFailureCount;
    private int advancingBuntCount;
    private int squeezeBuntCount;
    private int advancingBuntFailureCount;
    private int squeezeBuntFailureCount;
    private int stealToSecondCount;
    private int stealToThirdCount;

    @Override
    public void onGameCompleted(long totalScore, GameStatistics gameStatistics) {
        int score = Math.toIntExact(totalScore);
        scores.add(score);
        this.totalScore += score;
        scoreDistribution.merge(score, 1, Integer::sum);
        hitCount += gameStatistics.hitCount();
        singleHitCount += gameStatistics.singleHitCount();
        doubleHitCount += gameStatistics.doubleHitCount();
        tripleHitCount += gameStatistics.tripleHitCount();
        homeRunCount += gameStatistics.homeRunCount();
        soloHomeRunCount += gameStatistics.soloHomeRunCount();
        twoRunHomeRunCount += gameStatistics.twoRunHomeRunCount();
        threeRunHomeRunCount += gameStatistics.threeRunHomeRunCount();
        grandSlamCount += gameStatistics.grandSlamCount();
        buntCount += gameStatistics.buntCount();
        stealCount += gameStatistics.stealCount();
        buntFailureCount += gameStatistics.buntFailureCount();
        stealFailureCount += gameStatistics.stealFailureCount();
        advancingBuntCount += gameStatistics.advancingBuntCount();
        squeezeBuntCount += gameStatistics.squeezeBuntCount();
        advancingBuntFailureCount += gameStatistics.advancingBuntFailureCount();
        squeezeBuntFailureCount += gameStatistics.squeezeBuntFailureCount();
        stealToSecondCount += gameStatistics.stealToSecondCount();
        stealToThirdCount += gameStatistics.stealToThirdCount();
    }

    /**
     * Creates immutable aggregate statistics from all observed completed games.
     *
     * @return aggregate score and play statistics
     * @throws IllegalArgumentException when no completed game has been observed
     */
    public ScoreStatistics toScoreStatistics() {
        if (scores.isEmpty()) {
            throw new IllegalArgumentException("scores must not be empty");
        }
        scores.sort(Integer::compareTo);
        int middleIndex = scores.size() / 2;
        double median =
                scores.size() % 2 == 0
                        ? (scores.get(middleIndex - 1) + scores.get(middleIndex)) / 2.0
                        : scores.get(middleIndex);
        return ScoreStatisticsBuilder.scoreStatistics()
                .averageScore(totalScore / (double) scores.size())
                .medianScore(median)
                .maximumScore(scores.getLast())
                .gameCount(scores.size())
                .scoreDistribution(new LinkedHashMap<>(scoreDistribution))
                .hitCount(hitCount)
                .singleHitCount(singleHitCount)
                .doubleHitCount(doubleHitCount)
                .tripleHitCount(tripleHitCount)
                .homeRunCount(homeRunCount)
                .soloHomeRunCount(soloHomeRunCount)
                .twoRunHomeRunCount(twoRunHomeRunCount)
                .threeRunHomeRunCount(threeRunHomeRunCount)
                .grandSlamCount(grandSlamCount)
                .buntCount(buntCount)
                .stealCount(stealCount)
                .buntFailureCount(buntFailureCount)
                .stealFailureCount(stealFailureCount)
                .advancingBuntCount(advancingBuntCount)
                .squeezeBuntCount(squeezeBuntCount)
                .advancingBuntFailureCount(advancingBuntFailureCount)
                .squeezeBuntFailureCount(squeezeBuntFailureCount)
                .stealToSecondCount(stealToSecondCount)
                .stealToThirdCount(stealToThirdCount)
                .build();
    }
}
