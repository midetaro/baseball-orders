package com.example.baseballorders.simulator.domain.game.capability;

import com.example.baseballorders.simulator.domain.game.BasesState;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.player.BatterEntity;

/** 犠打を試みられる走者配置の能力。 */
public sealed interface Buntable extends BasesState permits AdvancingBuntable, SqueezeBuntable {

    /**
     * 現在の走者配置で打者にバントを試みさせる。
     *
     * @param batter バントを試みる打者
     * @return 打者のバント結果
     */
    BuntResult bunt(BatterEntity batter);

    /**
     * バントを試みないため状態を維持する。
     *
     * <p>シチュエーションに関わらず同じ結果のためConcreteStateクラスには実装させない。
     */
    default void buntNotTry() {}

    /** バント種別に応じた失敗結果を適用する。 */
    void buntFailure();

    /** バント種別に応じた成功結果を適用する。 */
    void buntSuccess();
}
