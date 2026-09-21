package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.BatterEntity;

/** 走者配置に応じたプレーを処理するStateの契約。 */
public interface BasesState {

    /** 現在のアウト数を返す。 @return イニングのアウト数 */
    OutCount getOutCount();

    /**
     * 指定した塁の走者を返す。
     *
     * @param base 確認する塁
     * @return 指定した塁の走者。走者がいない場合は {@code null}
     */
    BatterEntity runnerAt(Base base);

    /** 現在の走者数を返す。 @return 塁上の走者数 */
    int runnerCount();

    /**
     * 指定した塁の走者の有無を返す。
     *
     * @param base 確認する塁
     * @return 走者がいればtrue
     */
    boolean isOccupied(Base base);

    /** アウトを一つ加算し、三死ならイニング状態を初期化して試合を進める。 */
    void out();

    /** 打撃による凡退を適用し、三死目でなければ一定確率で先頭走者だけを進める。 */
    void battingOut();

    /**
     * 四球を適用し、一塁から連続して埋まっている走者だけを押し出す。
     *
     * @param batter 四球の打者
     */
    void walk(BatterEntity batter);

    /** 単打による進塁・得点を適用する。 @param batter 単打を打った打者 */
    void hitSingle(BatterEntity batter);

    /** 二塁打による進塁・得点を適用する。 @param batter 二塁打を打った打者 */
    void hitDouble(BatterEntity batter);

    /** 三塁打による進塁・得点を適用する。 @param batter 三塁打を打った打者 */
    void hitTriple(BatterEntity batter);

    /** 全走者と打者を生還させ、走者なしへ遷移する。 */
    void hitHomer();
}
