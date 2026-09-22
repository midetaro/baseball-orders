package com.example.baseballorders.simulator.domain.game;

/** 試合ごとのStateを初期生成する、状態を持たないファクトリ。 */
public class BaseStateFactory {
    /**
     * 走者配置に対応するStateを生成する。
     *
     * @param context 所属するイニング状態
     * @param configuration 一塁・二塁・三塁の走者配置ビットマスク
     * @return 指定された試合に所属するState
     */
    public BasesState create(InningStateContext context, int configuration) {
        return switch (configuration) {
            case 0 -> new NoBasesState(context);
            case 1 -> new SingleBasesState(context);
            case 2 -> new DoubleBaseState(context);
            case 3 -> new FirstDoubleBaseState(context);
            case 4 -> new ThirdBaseState(context);
            case 5 -> new FirstThirdBaseState(context);
            case 6 -> new DoubleThirdBaseState(context);
            case 7 -> new FullBasesState(context);
            default -> throw new IllegalArgumentException("不正な走者配置: " + configuration);
        };
    }
}
