package com.example.baseballorders.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * simulatorからbackendへ返す共有結果メッセージ。
 *
 * @param simulationId 要求と同じ相関ID
 * @param version メッセージスキーマのバージョン
 * @param gameScoreStatistics 全試合の得点統計
 * @param gameContentStatistics 全試合のプレー内容統計
 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationResultMessage(
        @JsonProperty("simulation_id") UUID simulationId,
        String version,
        GameScoreStatistics gameScoreStatistics,
        GameContentStatistics gameContentStatistics) {

    /** 安打内訳を含む得点・プレー内容結果メッセージのスキーマバージョン。 */
    public static final String CURRENT_VERSION = "3";

    /**
     * 全試合の得点統計。
     *
     * @param averageScore 平均得点
     * @param medianScore 中央値得点
     * @param maximumScore 最大得点
     * @param gameCount シミュレーションした試合数
     * @param scoreDistribution 得点ごとの試合数
     */
    public record GameScoreStatistics(
            double averageScore,
            double medianScore,
            int maximumScore,
            int gameCount,
            Map<Integer, Integer> scoreDistribution) {}

    /**
     * 全試合のプレー内容統計。
     *
     * @param hitCount 総安打数
     * @param singleHitCount 一塁打数
     * @param doubleHitCount 二塁打数
     * @param tripleHitCount 三塁打数
     * @param homeRunCount 本塁打数
     * @param soloHomeRunCount ソロ本塁打数
     * @param twoRunHomeRunCount ツーラン本塁打数
     * @param threeRunHomeRunCount スリーラン本塁打数
     * @param grandSlamCount 満塁本塁打数
     * @param buntCount 成功バント数
     * @param stealCount 成功盗塁数
     * @param buntFailureCount 失敗バント数
     * @param stealFailureCount 失敗盗塁数
     * @param advancingBuntCount 進塁バント成功数
     * @param squeezeBuntCount スクイズ成功数
     * @param advancingBuntFailureCount 進塁バント失敗数
     * @param squeezeBuntFailureCount スクイズ失敗数
     * @param stealToSecondCount 二盗成功数
     * @param stealToThirdCount 三盗成功数
     */
    public record GameContentStatistics(
            int hitCount,
            int singleHitCount,
            int doubleHitCount,
            int tripleHitCount,
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
            int stealToThirdCount) {}

    /**
     * Creates a result message with no statistics while discarding legacy per-game results.
     *
     * @param simulationId 要求と同じ相関ID
     * @param version メッセージスキーマのバージョン
     * @param ignoredResults 廃止された試合ごとの結果
     */
    public SimulationResultMessage(UUID simulationId, String version, List<Result> ignoredResults) {
        this(simulationId, version, null, null);
    }

    /** 廃止された1試合の結果を表す移行用型。 */
    public record Result(int score, int runs) {}
}
