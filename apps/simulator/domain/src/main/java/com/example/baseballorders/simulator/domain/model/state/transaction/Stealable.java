package com.example.baseballorders.simulator.domain.model.state.transaction;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;

/** 盗塁を試みる走者と進塁経路を表す塁状態の能力。 */
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
}
