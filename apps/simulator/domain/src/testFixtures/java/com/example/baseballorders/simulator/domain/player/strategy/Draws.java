package com.example.baseballorders.simulator.domain.player.strategy;

/**
 * 基準打者に対する名前付き乱数定数。
 *
 * <p>基準打者は打率 0.400 で、固定された盗塁成功率 0.70・進塁バント成功率 0.81・スクイズ企図率 0.25・スクイズ成功率 0.45 の設定を用いる {@code
 * MiddleDistanceHittingStrategy} / {@code StandardBuntStrategy} / {@code StandardStealStrategy}
 * を持つ（{@code BatterTestData#referenceBatter()}）。
 *
 * <p>同じ {@code 0.90f} が「二盗成功」と「バント失敗」を同時に意味するとおり、値の意味は どの戦略がその乱数を引いたかという文脈に依存する。だから生の float
 * ではなく名前付き定数でシナリオを書く。 各定数が表どおりの結果になることは {@code DrawsSelfTest} が検証する。
 *
 * <p>スクイズは企図（挑戦）ロールと成否ロールの 2 回の乱数を消費する。{@link #BUNT_SUCCESS} / {@link #BUNT_FAILURE} は 進塁バント成功率
 * 0.81 とスクイズ成功率 0.45 のどちらの成否ロールとしても成立するしきい値（0.10 / 0.90）を選んでいるため、 成否ロールにはバント種別を問わず同じ定数を使い回せる。
 */
public final class Draws {

    /** 四球になる乱数。 */
    public static final float WALK = 0.04f;

    /** 単打になる乱数。 */
    public static final float SINGLE = 0.30f;

    /** 二塁打になる乱数。 */
    public static final float DOUBLE = 0.37f;

    /** 三塁打になる乱数。 */
    public static final float TRIPLE = 0.395f;

    /** 本塁打になる乱数。 */
    public static final float HOMER = 0.42f;

    /** 三振になる乱数。 */
    public static final float STRIKEOUT = 0.45f;

    /** 凡退になる乱数。 */
    public static final float BATTED_OUT = 0.60f;

    /** 凡退時に先頭走者が進む乱数。一塁・二塁の 20% と三塁の 10% をすべて満たす。 */
    public static final float ADVANCE = 0.05f;

    /** 凡退時に先頭走者が進まない乱数。 */
    public static final float NO_ADVANCE = 0.50f;

    /** バントの成否ロールで成功になる乱数。進塁バント成功率 0.81 未満かつスクイズ成功率 0.45 未満なので、 バント種別を問わず成否ロールとして使える。 */
    public static final float BUNT_SUCCESS = 0.10f;

    /** バントの成否ロールで失敗になる乱数。進塁バント成功率 0.81 以上かつスクイズ成功率 0.45 以上なので、 バント種別を問わず成否ロールとして使える。 */
    public static final float BUNT_FAILURE = 0.90f;

    /** スクイズを企図（挑戦）する乱数。企図率 0.25 未満。 */
    public static final float SQUEEZE_CHALLENGE_TRY = 0.05f;

    /** スクイズを企図（挑戦）しない乱数。企図率 0.25 以上。 */
    public static final float SQUEEZE_CHALLENGE_NOT_TRY = 0.50f;

    /** 二盗を試みない乱数。標準戦略の試行境界 0.70 未満。 */
    public static final float STEAL_TO_SECOND_NOT_TRY = 0.50f;

    /** 二盗が成功する乱数。標準戦略の 0.70 超 0.91 未満。 */
    public static final float STEAL_TO_SECOND_SUCCESS = 0.90f;

    /** 二盗が失敗する乱数。標準戦略の成功上限 0.91 以上。 */
    public static final float STEAL_TO_SECOND_FAILURE = 0.98f;

    /** 三盗を試みない乱数。標準戦略の試行境界 0.90 未満。 */
    public static final float STEAL_TO_THIRD_NOT_TRY = 0.50f;

    /** 三盗が成功する乱数。標準戦略の 0.90 超 0.97 未満。 */
    public static final float STEAL_TO_THIRD_SUCCESS = 0.95f;

    /** 三盗が失敗する乱数。標準戦略の成功上限 0.97 以上。 */
    public static final float STEAL_TO_THIRD_FAILURE = 0.995f;

    private Draws() {}
}
