package com.example.baseballorders.simulator.domain.game.capability;

/** 走者を進塁させるバントを試みられる走者配置の能力。 */
public non-sealed interface AdvancingBuntable extends Buntable {

    /** 進塁バントの対象走者を一つ先の塁へ進める。 */
    void advanceRunnersByBunt();

    /** 進塁バント失敗として打者の一死を加算する。 */
    default void buntFailure() {
        out();
    }

    /** 進塁バント成功として走者を進め、打者の一死を加算する。 */
    default void buntSuccess() {
        advanceRunnersByBunt();
        out();
    }
}
