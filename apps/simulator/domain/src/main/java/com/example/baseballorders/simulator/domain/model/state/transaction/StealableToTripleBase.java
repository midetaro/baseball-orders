package com.example.baseballorders.simulator.domain.model.state.transaction;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;

/** 二塁走者が三塁へ盗塁できる塁状態。 */
public non-sealed interface StealableToTripleBase extends Stealable {

    /**
     * 盗塁を試みる二塁走者を返す。
     *
     * @return 二塁走者。走者が設定されていない場合は {@code null}
     */
    BatterEntity runnerOnSecond();

    /**
     * 盗塁を試みる走者を返す。
     *
     * @return 二塁走者
     */
    @Override
    default BatterEntity runner() {
        return runnerOnSecond();
    }

    /**
     * 盗塁前の塁を返す。
     *
     * @return 二塁
     */
    @Override
    default Base sourceBase() {
        return Base.SECOND;
    }

    /**
     * 盗塁先の塁を返す。
     *
     * @return 三塁
     */
    @Override
    default Base targetBase() {
        return Base.THIRD;
    }
}
