package com.example.baseballorders.simulator.domain.player.strategy;

/**
 * 基準打者に対する名前付き乱数定数。
 *
 * <p>基準打者は出塁率 0.400・長打率 0.550・バント成功率 0.700・盗塁成功率 0.800 で、 {@code MiddleDistanceHittingStrategy} /
 * {@code StandardBuntStrategy} / {@code StandardStealStrategy} を持つ（{@code
 * BatterTestData#referenceBatter()}）。
 *
 * <p>同じ {@code 0.90f} が「二盗成功」と「バント失敗」を同時に意味するとおり、値の意味は どの戦略がその乱数を引いたかという文脈に依存する。だから生の float
 * ではなく名前付き定数でシナリオを書く。 各定数が表どおりの結果になることは {@code DrawsSelfTest} が検証する。
 */
public final class Draws {

    /** 四球になる乱数。 */
    public static final float WALK = 0.04f;

    /** 単打になる乱数。 */
    public static final float SINGLE = 0.30f;

    /** 二塁打になる乱数。 */
    public static final float DOUBLE = 0.34f;

    /** 三塁打になる乱数。 */
    public static final float TRIPLE = 0.36f;

    /** 本塁打になる乱数。 */
    public static final float HOMER = 0.39f;

    /** 三振になる乱数。 */
    public static final float STRIKEOUT = 0.45f;

    /** 凡退になる乱数。 */
    public static final float BATTED_OUT = 0.60f;

    /** 凡退時に先頭走者が進む乱数。一塁・二塁の 20% と三塁の 10% をすべて満たす。 */
    public static final float ADVANCE = 0.05f;

    /** 凡退時に先頭走者が進まない乱数。 */
    public static final float NO_ADVANCE = 0.50f;

    /** バントが成功する乱数。成功率 0.700 未満。 */
    public static final float BUNT_SUCCESS = 0.10f;

    /** バントが失敗する乱数。成功率 0.700 以上。 */
    public static final float BUNT_FAILURE = 0.90f;

    /** 二盗を試みない乱数。標準戦略の試行境界 0.80 未満。 */
    public static final float STEAL_TO_SECOND_NOT_TRY = 0.50f;

    /** 二盗が成功する乱数。標準戦略の 0.80 超 0.96 未満。 */
    public static final float STEAL_TO_SECOND_SUCCESS = 0.90f;

    /** 二盗が失敗する乱数。標準戦略の成功上限 0.96 以上。 */
    public static final float STEAL_TO_SECOND_FAILURE = 0.98f;

    /** 三盗を試みない乱数。標準戦略の試行境界 0.95 未満。 */
    public static final float STEAL_TO_THIRD_NOT_TRY = 0.50f;

    /** 三盗が成功する乱数。標準戦略の 0.95 超 0.99 未満。 */
    public static final float STEAL_TO_THIRD_SUCCESS = 0.97f;

    /** 三盗が失敗する乱数。標準戦略の成功上限 0.99 以上。 */
    public static final float STEAL_TO_THIRD_FAILURE = 0.995f;

    private Draws() {}
}
