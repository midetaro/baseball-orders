package com.example.baseballorders.simulator.domain.game;

/**
 * 1 イニングの脚本。
 *
 * <p>試合レベルのシナリオでは「このイニングで何点入り、何打席でイニングが終わるか」だけを決め、 走者配置ごとの進塁規則はフェイクへ委ねる。進塁規則そのものは L2 の各 {@code
 * BasesState} テストが担保する。
 *
 * @param inning このイニングの回数。脚本と試合の進行がずれていないことを検証するために使う
 * @param runs このイニングで加算する得点
 * @param outs このイニングを終えるまでに消費する打席数。1 以上 3 以下
 */
public record InningPlan(int inning, long runs, int outs) {

    /** 1 イニングの標準的なアウト数。 */
    public static final int OUTS_PER_INNING = 3;

    /**
     * 3 アウトでイニングを終える脚本を作成する。
     *
     * @param inning 回数
     * @param runs このイニングの得点
     * @return イニングの脚本
     */
    public static InningPlan scoring(int inning, long runs) {
        return new InningPlan(inning, runs, OUTS_PER_INNING);
    }

    /**
     * 3 アウトでイニングを終える無得点の脚本を作成する。
     *
     * @param inning 回数
     * @return イニングの脚本
     */
    public static InningPlan scoreless(int inning) {
        return scoring(inning, 0);
    }

    /**
     * 回数・得点・打席数を検証したうえでイニングの脚本を作成する。
     *
     * @param inning 回数
     * @param runs このイニングの得点
     * @param outs このイニングを終えるまでに消費する打席数
     * @throws IllegalArgumentException 回数が 1 以上 9 以下でない場合、得点が負の場合、または打席数が 1 以上 3 以下でない場合
     */
    public InningPlan {
        if (inning < 1 || inning > 9) {
            throw new IllegalArgumentException("回数は1以上9以下でなければならない: " + inning);
        }
        if (runs < 0) {
            throw new IllegalArgumentException("得点は0以上でなければならない: " + runs);
        }
        if (outs < 1 || outs > OUTS_PER_INNING) {
            throw new IllegalArgumentException("打席数は1以上3以下でなければならない: " + outs);
        }
    }
}
