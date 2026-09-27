package com.example.baseballorders.backend.domain;

import java.util.Objects;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * 1試合実行における1つの状況推移。
 *
 * @param inning イニング
 * @param actionResult 打者または走者の振る舞い結果
 * @param outCount アウトカウント
 * @param cumulativeScore 累積得点
 * @param runnerState 走者状況
 */
@Builder(style = BuilderStyle.STAGED)
public record GameTransition(
        long inning, String actionResult, int outCount, long cumulativeScore, String runnerState) {

    /**
     * Requires the textual fields describing the transition.
     *
     * @throws NullPointerException when actionResult or runnerState is null
     */
    public GameTransition {
        Objects.requireNonNull(actionResult, "actionResult must not be null");
        Objects.requireNonNull(runnerState, "runnerState must not be null");
    }
}
