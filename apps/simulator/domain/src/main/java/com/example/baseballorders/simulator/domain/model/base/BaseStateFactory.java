package com.example.baseballorders.simulator.domain.model.base;

import com.example.baseballorders.simulator.domain.model.GameBattingContext;

/** 試合ごとのStateを初期生成する、状態を持たないファクトリ。 */
public class BaseStateFactory {
    /**
     * NoBasesStateを生成する。
     *
     * @param context 所属する試合
     * @param inningState 試合内で共有するイニング状態
     * @return 指定された試合に所属するState
     */
    public NoBasesState createNoBasesState(GameBattingContext context, InningState inningState) {
        return new NoBasesState(context, inningState);
    }

    /**
     * SingleBasesStateを生成する。
     *
     * @param context 所属する試合
     * @param inningState 試合内で共有するイニング状態
     * @return 指定された試合に所属するState
     */
    public SingleBasesState createSingleBasesState(
            GameBattingContext context, InningState inningState) {
        return new SingleBasesState(context, inningState);
    }

    /**
     * DoubleBaseStateを生成する。
     *
     * @param context 所属する試合
     * @param inningState 試合内で共有するイニング状態
     * @return 指定された試合に所属するState
     */
    public DoubleBaseState createDoubleBaseState(
            GameBattingContext context, InningState inningState) {
        return new DoubleBaseState(context, inningState);
    }

    /**
     * FirstDoubleBaseStateを生成する。
     *
     * @param context 所属する試合
     * @param inningState 試合内で共有するイニング状態
     * @return 指定された試合に所属するState
     */
    public FirstDoubleBaseState createFirstDoubleBaseState(
            GameBattingContext context, InningState inningState) {
        return new FirstDoubleBaseState(context, inningState);
    }

    /**
     * ThirdBaseStateを生成する。
     *
     * @param context 所属する試合
     * @param inningState 試合内で共有するイニング状態
     * @return 指定された試合に所属するState
     */
    public ThirdBaseState createThirdBaseState(
            GameBattingContext context, InningState inningState) {
        return new ThirdBaseState(context, inningState);
    }

    /**
     * FirstThirdBaseStateを生成する。
     *
     * @param context 所属する試合
     * @param inningState 試合内で共有するイニング状態
     * @return 指定された試合に所属するState
     */
    public FirstThirdBaseState createFirstThirdBaseState(
            GameBattingContext context, InningState inningState) {
        return new FirstThirdBaseState(context, inningState);
    }

    /**
     * DoubleThirdBaseStateを生成する。
     *
     * @param context 所属する試合
     * @param inningState 試合内で共有するイニング状態
     * @return 指定された試合に所属するState
     */
    public DoubleThirdBaseState createDoubleThirdBaseState(
            GameBattingContext context, InningState inningState) {
        return new DoubleThirdBaseState(context, inningState);
    }

    /**
     * FullBasesStateを生成する。
     *
     * @param context 所属する試合
     * @param inningState 試合内で共有するイニング状態
     * @return 指定された試合に所属するState
     */
    public FullBasesState createFullBasesState(
            GameBattingContext context, InningState inningState) {
        return new FullBasesState(context, inningState);
    }
}
