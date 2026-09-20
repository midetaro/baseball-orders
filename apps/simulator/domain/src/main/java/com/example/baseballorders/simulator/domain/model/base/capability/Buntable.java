package com.example.baseballorders.simulator.domain.model.base.capability;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;

/** 犠打を試みられる走者配置の能力。 */
public sealed interface Buntable permits AdvancingBuntable,SqueezeBuntable {

    /**
     * 現在の走者配置で打者にバントを試みさせる。
     *
     * @param batter バントを試みる打者
     * @return 打者のバント結果
     */
    BuntResult bunt(BatterEntity batter);

    /**
     * バントを試みないため状態を維持する。
     * <p>シチュエーションに関わらず同じ結果のためConcreteStateクラスには実装させない。</p>
     * */
    void buntNotTry();

    /**
     * バント失敗で一死を加算する。
     * <p>シチュエーションに関わらず同じ結果のためConcreteStateクラスには実装させない。</p>
     * */
    void buntFailure();

    /** 犠打で走者を進めて一死を加算する。 */
    void buntSuccess();
}
