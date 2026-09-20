package com.example.baseballorders.simulator.domain.game.capability;

import com.example.baseballorders.simulator.domain.play.OutCount;

/** スクイズを試みられる走者配置の能力。 */
public non-sealed interface SqueezeBuntable extends Buntable {

    /** スクイズ失敗でアウトになった三塁走者を取り除く。 */
    void retireRunnerOnThird();

    /** スクイズ成功で三塁走者を生還させる。 */
    void scoreRunnerOnThird();

    /** スクイズ失敗として三塁走者と打者の二死を適用する。 */
    default void buntFailure() {
        retireRunnerOnThird();
        out();
        if (getOutCount() != OutCount.NO_OUT) {
            out();
        }
    }

    /** スクイズ成功として三塁走者を生還させ、打者の一死を加算する。 */
    default void buntSuccess() {
        scoreRunnerOnThird();
        out();
    }
}
