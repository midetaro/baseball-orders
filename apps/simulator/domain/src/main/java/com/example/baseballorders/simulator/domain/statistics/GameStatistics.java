package com.example.baseballorders.simulator.domain.statistics;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** Statistics recorded during one simulated game. */
@Builder(style = BuilderStyle.STAGED)
public record GameStatistics(
        int homeRunCount,
        int soloHomeRunCount,
        int twoRunHomeRunCount,
        int threeRunHomeRunCount,
        int grandSlamCount,
        int buntCount,
        int stealCount,
        int buntFailureCount,
        int stealFailureCount,
        int advancingBuntCount,
        int squeezeBuntCount,
        int advancingBuntFailureCount,
        int squeezeBuntFailureCount,
        int stealToSecondCount,
        int stealToThirdCount) {

    /**
     * Creates statistics with no tactical-play classification.
     *
     * @param homeRunCount home run count
     * @param soloHomeRunCount solo home run count
     * @param twoRunHomeRunCount two-run home run count
     * @param threeRunHomeRunCount three-run home run count
     * @param grandSlamCount grand slam count
     * @param buntCount successful bunt count
     * @param stealCount successful steal count
     * @param buntFailureCount failed bunt count
     * @param stealFailureCount failed steal count
     */
    public GameStatistics(
            int homeRunCount,
            int soloHomeRunCount,
            int twoRunHomeRunCount,
            int threeRunHomeRunCount,
            int grandSlamCount,
            int buntCount,
            int stealCount,
            int buntFailureCount,
            int stealFailureCount) {
        this(
                homeRunCount,
                soloHomeRunCount,
                twoRunHomeRunCount,
                threeRunHomeRunCount,
                grandSlamCount,
                buntCount,
                stealCount,
                buntFailureCount,
                stealFailureCount,
                0,
                0,
                0,
                0,
                0,
                0);
    }
}
