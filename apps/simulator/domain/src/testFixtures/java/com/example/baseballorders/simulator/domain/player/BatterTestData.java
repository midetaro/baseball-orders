package com.example.baseballorders.simulator.domain.player;

import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StealStrategy;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import java.util.Collections;
import java.util.List;

/**
 * シナリオテスト用の基準打者と打順を提供する。
 *
 * <p>ここで定義する基準成績が {@code Draws} の名前付き乱数定数の前提になる。成績を変えると {@code Draws} の意味が変わるので、変更する場合は {@code
 * DrawsSelfTest} も同時に更新する。
 */
public final class BatterTestData {

    /** 基準打者の出塁率。 */
    public static final float ON_BASE_PERCENTAGE = 0.400f;

    /** 基準打者の長打率。 */
    public static final float SLUGGING = 0.550f;

    /** 基準打者のバント成功率。 */
    public static final float BUNT_SUCCESS_RATE = 0.700f;

    /** 基準打者の盗塁成功率。 */
    public static final float STEAL_SUCCESS_RATE = 0.800f;

    /** 打順の人数。 */
    public static final int LINE_UP_SIZE = 9;

    /**
     * {@link #distinctLineUp()} の打順を観測するための乱数。
     *
     * <p>1 番打者は本塁打、2 番以降は三振になるので、この乱数だけで打順の位置が判別できる。
     */
    public static final float DISTINCT_LINE_UP_DRAW = 0.35f;

    private BatterTestData() {}

    /**
     * {@code Draws} の前提となる基準打者を作成する。
     *
     * @return 中距離打撃・標準盗塁・標準バントの基準打者
     */
    public static BatterEntity referenceBatter() {
        return batter(
                SimulationRulesTestData.strategies().middleDistanceHittingStrategy(),
                SimulationRulesTestData.strategies().standardSteal(),
                SimulationRulesTestData.strategies().standardBunt());
    }

    /**
     * 基準成績のまま戦略だけを差し替えた打者を作成する。
     *
     * @param hittingStrategy 打撃戦略
     * @param stealStrategy 盗塁戦略
     * @param buntStrategy バント戦略
     * @return 基準成績の打者
     */
    public static BatterEntity batter(
            HittingStrategy hittingStrategy,
            StealStrategy stealStrategy,
            BuntStrategy buntStrategy) {
        return batter(ON_BASE_PERCENTAGE, SLUGGING, hittingStrategy, stealStrategy, buntStrategy);
    }

    /**
     * 成績と戦略を指定して打者を作成する。
     *
     * @param onBasePercentage 出塁率
     * @param slugging 長打率
     * @param hittingStrategy 打撃戦略
     * @param stealStrategy 盗塁戦略
     * @param buntStrategy バント戦略
     * @return 指定された打者
     */
    public static BatterEntity batter(
            float onBasePercentage,
            float slugging,
            HittingStrategy hittingStrategy,
            StealStrategy stealStrategy,
            BuntStrategy buntStrategy) {
        return new BatterEntity(
                onBasePercentage,
                slugging,
                BUNT_SUCCESS_RATE,
                STEAL_SUCCESS_RATE,
                hittingStrategy,
                stealStrategy,
                buntStrategy);
    }

    /**
     * 乱数を引かない盗塁・バント戦略を持つ基準打者を作成する。
     *
     * <p>盗塁・バントフェーズが乱数を消費しないので、打撃だけを脚本にできる。
     *
     * @return 盗塁もバントもしない基準打者
     */
    public static BatterEntity swingOnlyBatter() {
        return batter(
                SimulationRulesTestData.strategies().middleDistanceHittingStrategy(),
                SimulationRulesTestData.strategies().noSteal(),
                SimulationRulesTestData.strategies().noBunt());
    }

    /**
     * 基準打者 9 人の打順を作成する。
     *
     * @return 全員が基準打者の打順
     */
    public static LineUpEntity uniformLineUp() {
        return lineUpOf(referenceBatter());
    }

    /**
     * 盗塁もバントもしない基準打者 9 人の打順を作成する。
     *
     * @return 全員が打撃だけを行う打順
     */
    public static LineUpEntity swingOnlyLineUp() {
        return lineUpOf(swingOnlyBatter());
    }

    /**
     * 同じ打者 9 人の打順を作成する。
     *
     * @param batter 全打順に並べる打者
     * @return 9 人の打順
     */
    public static LineUpEntity lineUpOf(BatterEntity batter) {
        return new LineUpEntity(Collections.nCopies(LINE_UP_SIZE, batter));
    }

    /**
     * 指定された 9 人の打順を作成する。
     *
     * @param batters 打順に並べる 9 人
     * @return 9 人の打順
     * @throws IllegalArgumentException 9 人でない場合
     */
    public static LineUpEntity lineUpOf(BatterEntity... batters) {
        if (batters.length != LINE_UP_SIZE) {
            throw new IllegalArgumentException("打順は9人でなければならない: " + batters.length);
        }
        return new LineUpEntity(List.of(batters));
    }

    /**
     * 打順上の位置を観測できる打順を作成する。
     *
     * <p>{@link #DISTINCT_LINE_UP_DRAW} を引いたとき、1 番打者だけが本塁打になり 2 番以降は三振になる。
     * 同じ乱数の解釈が打者ごとに変わるので、打順が何番へ回ったかを得点と統計から判別できる。
     *
     * @return 1 番打者だけが本塁打を打つ打順
     */
    public static LineUpEntity distinctLineUp() {
        BatterEntity leadOff =
                batter(
                        SimulationRulesTestData.strategies().longDistanceAtBat(),
                        SimulationRulesTestData.strategies().noSteal(),
                        SimulationRulesTestData.strategies().noBunt());
        BatterEntity strikeOut =
                batter(
                        0.300f,
                        0.300f,
                        SimulationRulesTestData.strategies().middleDistanceHittingStrategy(),
                        SimulationRulesTestData.strategies().noSteal(),
                        SimulationRulesTestData.strategies().noBunt());
        return lineUpOf(
                leadOff, strikeOut, strikeOut, strikeOut, strikeOut, strikeOut, strikeOut,
                strikeOut, strikeOut);
    }
}
