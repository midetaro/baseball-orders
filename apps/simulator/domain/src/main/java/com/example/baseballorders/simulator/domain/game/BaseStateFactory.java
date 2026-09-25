package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.rule.RunnerAdvanceProbabilities;
import lombok.RequiredArgsConstructor;

/** 試合ごとのStateを初期生成する、状態を持たないファクトリ。 */
@RequiredArgsConstructor
public class BaseStateFactory {

    /** 生成するStateへ渡す凡退時の進塁確率。設定から供給される。 */
    private final RunnerAdvanceProbabilities runnerAdvanceProbabilities;

    /**
     * 走者配置に対応するStateを生成する。
     *
     * @param context 所属するイニング状態
     * @param configuration 一塁・二塁・三塁の走者配置ビットマスク
     * @return 指定された試合に所属するState
     */
    public BasesState create(InningStateContext context, int configuration) {
        return switch (configuration) {
            case 0 -> new NoBasesState(context, runnerAdvanceProbabilities);
            case 1 -> new SingleBasesState(context, runnerAdvanceProbabilities);
            case 2 -> new DoubleBaseState(context, runnerAdvanceProbabilities);
            case 3 -> new FirstDoubleBaseState(context, runnerAdvanceProbabilities);
            case 4 -> new ThirdBaseState(context, runnerAdvanceProbabilities);
            case 5 -> new FirstThirdBaseState(context, runnerAdvanceProbabilities);
            case 6 -> new DoubleThirdBaseState(context, runnerAdvanceProbabilities);
            case 7 -> new FullBasesState(context, runnerAdvanceProbabilities);
            default -> throw new IllegalArgumentException("不正な走者配置: " + configuration);
        };
    }
}
