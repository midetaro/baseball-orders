package com.example.baseballorders.simulator.domain.model.statistics;

/** 1試合で発生したプレーを集計し、その時点の試合統計を生成する。 */
public final class GameStatisticsRecorder {

    private int homeRunCount;
    private int soloHomeRunCount;
    private int twoRunHomeRunCount;
    private int threeRunHomeRunCount;
    private int grandSlamCount;
    private int buntCount;
    private int stealCount;

    /**
     * 塁上の走者数に応じた本塁打の種類と本塁打総数を記録する。
     *
     * @param runnerCount 本塁打直前の走者数（0～3）
     * @throws IllegalStateException 走者数が0～3以外の場合
     */
    public void recordHomeRun(int runnerCount) {
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

    /** 成功したバントを一つ記録する。 */
    public void recordBunt() {
        buntCount++;
    }

    /** 成功した盗塁を一つ記録する。 */
    public void recordSteal() {
        stealCount++;
    }

    /**
     * 現在までに記録された値から不変の試合統計を生成する。
     *
     * @return この記録器の現在値を写した試合統計
     */
    public GameStatistics snapshot() {
        return new GameStatistics(
                homeRunCount,
                soloHomeRunCount,
                twoRunHomeRunCount,
                threeRunHomeRunCount,
                grandSlamCount,
                buntCount,
                stealCount);
    }
}
