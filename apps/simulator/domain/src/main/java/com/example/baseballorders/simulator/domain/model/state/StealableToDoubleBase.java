package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

/** 一塁走者が二塁へ盗塁できる塁状態。 */
public non-sealed interface StealableToDoubleBase extends Stealable {

    /**
     * 盗塁を試みる一塁走者を返す。
     *
     * @return 一塁走者。走者が設定されていない場合は {@code null}
     */
    BatterEntity runnerOnFirst();

    /**
     * 盗塁を試みる走者を返す。
     *
     * @return 一塁走者
     */
    @Override
    default BatterEntity runner() {
        return runnerOnFirst();
    }

    /**
     * 盗塁前の塁を返す。
     *
     * @return 一塁
     */
    @Override
    default Base sourceBase() {
        return Base.FIRST;
    }

    /**
     * 盗塁先の塁を返す。
     *
     * @return 二塁
     */
    @Override
    default Base targetBase() {
        return Base.SECOND;
    }
}
