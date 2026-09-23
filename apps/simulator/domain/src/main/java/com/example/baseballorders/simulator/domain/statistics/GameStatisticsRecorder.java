package com.example.baseballorders.simulator.domain.statistics;

import com.example.baseballorders.simulator.domain.play.*;

/** 1試合で発生したプレーを集計し、その時点の試合統計を生成する。 */
public final class GameStatisticsRecorder implements PlayResultObserver {

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
    public void onBattingResult(BattingResult battingResult, int runnerCount) {
        switch (battingResult) {
            case STRIKEOUT, BATTED_OUT, WALK -> {}
            case HIT_SINGLE -> {
                hitCount++;
                singleHitCount++;
            }
            case HIT_DOUBLE -> {
                hitCount++;
                doubleHitCount++;
            }
            case HIT_TRIPLE -> {
                hitCount++;
                tripleHitCount++;
            }
            case HIT_HOMER -> {
                recordHomeRun(runnerCount);
                hitCount++;
            }
        }
    }

    @Override
    public void onBuntResult(BuntResult buntResult, BuntType buntType) {
        switch (buntResult) {
            case NOT_TRY -> {}
            case SUCCESS -> {
                buntCount++;
                switch (buntType) {
                    case ADVANCING -> advancingBuntCount++;
                    case SQUEEZE -> squeezeBuntCount++;
                }
            }
            case FAILURE -> {
                buntFailureCount++;
                switch (buntType) {
                    case ADVANCING -> advancingBuntFailureCount++;
                    case SQUEEZE -> squeezeBuntFailureCount++;
                }
            }
        }
    }

    @Override
    public void onStealResult(StealResult stealResult, StealTarget stealTarget) {
        switch (stealResult) {
            case NOT_TRY -> {}
            case SUCCESS -> {
                stealCount++;
                switch (stealTarget) {
                    case SECOND -> stealToSecondCount++;
                    case THIRD -> stealToThirdCount++;
                }
            }
            case FAILURE -> stealFailureCount++;
        }
    }

    private void recordHomeRun(int runnerCount) {
        Runnable recordHomeRunType =
                switch (runnerCount) {
                    case 0 -> () -> soloHomeRunCount++;
                    case 1 -> () -> twoRunHomeRunCount++;
                    case 2 -> () -> threeRunHomeRunCount++;
                    case 3 -> () -> grandSlamCount++;
                    default ->
                            throw new IllegalStateException("runner count must be between 0 and 3");
                };
        homeRunCount++;
        recordHomeRunType.run();
    }

    /**
     * 現在までに記録された値から不変の試合統計を生成する。
     *
     * @return この記録器の現在値を写した試合統計
     */
    public GameStatistics snapshot() {
        return GameStatisticsBuilder.gameStatistics()
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
