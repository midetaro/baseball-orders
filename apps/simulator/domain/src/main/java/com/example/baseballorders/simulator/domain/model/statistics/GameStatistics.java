package com.example.baseballorders.simulator.domain.model.statistics;

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
        int stealCount) {}
