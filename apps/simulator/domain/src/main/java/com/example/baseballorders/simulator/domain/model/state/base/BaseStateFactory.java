package com.example.baseballorders.simulator.domain.model.state.base;

import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import org.springframework.stereotype.Component;

/** 走者配置に対応する不変の塁状態を生成するファクトリ。 */
@Component
public class BaseStateFactory {

    /**
     * 走者のいない塁状態を生成する。
     *
     * @return 走者のいない塁状態
     */
    public BasesState empty() {
        return new NoBasesState(this);
    }

    /**
     * 指定された走者配置に対応する塁状態を生成する。
     *
     * @param first 一塁走者。いない場合は {@code null}
     * @param second 二塁走者。いない場合は {@code null}
     * @param third 三塁走者。いない場合は {@code null}
     * @return 指定された走者配置を表す塁状態
     */
    public BasesState create(BatterEntity first, BatterEntity second, BatterEntity third) {
        if (first != null && second != null && third != null) {
            return new FullBasesState(this, first, second, third);
        }
        if (first != null && second != null) {
            return new FirstDoubleBaseState(this, first, second);
        }
        if (first != null && third != null) {
            return new FirstThirdBaseState(this, first, third);
        }
        if (first != null) {
            return new SingleBasesState(this, first);
        }
        if (second != null && third != null) {
            return new DoubleThirdBaseState(this, second, third);
        }
        if (second != null) {
            return new DoubleBaseState(this, second);
        }
        if (third != null) {
            return new ThirdBaseState(this, third);
        }
        return empty();
    }
}
