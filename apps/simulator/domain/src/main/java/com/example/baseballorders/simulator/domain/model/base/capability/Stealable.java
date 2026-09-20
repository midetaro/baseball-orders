package com.example.baseballorders.simulator.domain.model.base.capability;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;

/**
 * 盗塁を試みる走者と進塁経路を表す塁状態の能力。
 *
 * <p>盗塁は、打者ではなくランナーの情報を受け取る必要がある。
 */
public sealed interface Stealable permits StealableToDoubleBase, StealableToTripleBase {

    /**
     * 盗塁を試みる走者を返す。
     *
     * @return 盗塁を試みる走者。走者が設定されていない場合は {@code null}
     */
    BatterEntity runner();

    /**
     * 盗塁前の塁を返す。
     *
     * @return 盗塁前の塁
     */
    Base sourceBase();

    /**
     * 盗塁先の塁を返す。
     *
     * @return 盗塁先の塁
     */
    Base targetBase();

    /** 一塁走者に二塁盗塁を試みさせる。 @return 走者の盗塁結果 */
    StealResult stealToDouble();

    /** 二塁走者に三塁盗塁を試みさせる。 @return 走者の盗塁結果 */
    StealResult stealToTriple();

    /** 盗塁を試みないため状態を維持する。 */
    void stealNotTry();

    /** 盗塁死の走者を除去して一死を加算する。 */
    void stealFailure();

    /** 盗塁対象の走者を次の塁へ移す。 */
    void stealSuccess();
}
