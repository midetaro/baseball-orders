package com.example.baseballorders.simulator.domain.model.base;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.base.capability.Buntable;
import com.example.baseballorders.simulator.domain.model.base.capability.Stealable;

import java.util.Optional;

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

    /** 現在のStateが持つ盗塁機会を返す。 @return 盗塁可能ならその能力 */
    Optional<Stealable> stealOpportunity();

    /** 現在のStateが持つ犠打機会を返す。 @return 犠打可能ならその能力 */
    Optional<Buntable> buntOpportunityByBase();

    /** アウトを一つ加算し、三死ならイニング状態を初期化して試合を進める。 */
    void out();

    /**
     * 現在の走者配置で打者にバントを試みさせる。
     *
     * @param batter バントを試みる打者
     * @return バント機会がある場合は打者のバント結果、それ以外は {@link BuntResult#NOT_TRY}
     */
    BuntResult bunt(BatterEntity batter);

    /** 単打による進塁・得点を適用する。 @param batter 単打を打った打者 */
    void hitSingle(BatterEntity batter);

    /** 二塁打による進塁・得点を適用する。 @param batter 二塁打を打った打者 */
    void hitDouble(BatterEntity batter);

    /** 三塁打による進塁・得点を適用する。 @param batter 三塁打を打った打者 */
    void hitTriple(BatterEntity batter);

    /** 全走者と打者を生還させ、走者なしへ遷移する。 */
    void hitHomer();

    /** 一塁走者に二塁盗塁を試みさせる。 @return 走者の盗塁結果。機会がなければNOT_TRY */
    StealResult stealToDouble();

    /** 二塁走者に三塁盗塁を試みさせる。 @return 走者の盗塁結果。機会がなければNOT_TRY */
    StealResult stealToTriple();

    /** バントを試みないため状態を維持する。 */
    void buntNotTry();

    /** バント失敗で一死を加算する。 @throws IllegalStateException 犠打機会がない場合 */
    void buntFailure();

    /** 犠打で走者を進めて一死を加算する。 @throws IllegalStateException 犠打機会がない場合 */
    void buntSuccess();

    /** 盗塁を試みないため状態を維持する。 */
    void stealNotTry();

    /** 盗塁死の走者を除去して一死を加算する。 @throws IllegalStateException 盗塁機会がない場合 */
    void stealFailure();

    /** 盗塁対象の走者を次の塁へ移す。 @throws IllegalStateException 盗塁機会がない場合 */
    void stealSuccess();
}
