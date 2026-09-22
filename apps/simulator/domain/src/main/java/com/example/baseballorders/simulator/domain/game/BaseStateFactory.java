package com.example.baseballorders.simulator.domain.game;

/** 試合ごとのStateを初期生成する、状態を持たないファクトリ。 */
public class BaseStateFactory {
    /**
     * NoBasesStateを生成する。
     *
     * @param context 所属するイニング状態
     * @return 指定された試合に所属するState
     */
    public NoBasesState createNoBasesState(InningStateContext context) {
        return new NoBasesState(context);
    }

    /**
     * SingleBasesStateを生成する。
     *
     * @param context 所属するイニング状態
     * @return 指定された試合に所属するState
     */
    public SingleBasesState createSingleBasesState(InningStateContext context) {
        return new SingleBasesState(context);
    }

    /**
     * DoubleBaseStateを生成する。
     *
     * @param context 所属するイニング状態
     * @return 指定された試合に所属するState
     */
    public DoubleBaseState createDoubleBaseState(InningStateContext context) {
        return new DoubleBaseState(context);
    }

    /**
     * FirstDoubleBaseStateを生成する。
     *
     * @param context 所属するイニング状態
     * @return 指定された試合に所属するState
     */
    public FirstDoubleBaseState createFirstDoubleBaseState(InningStateContext context) {
        return new FirstDoubleBaseState(context);
    }

    /**
     * ThirdBaseStateを生成する。
     *
     * @param context 所属するイニング状態
     * @return 指定された試合に所属するState
     */
    public ThirdBaseState createThirdBaseState(InningStateContext context) {
        return new ThirdBaseState(context);
    }

    /**
     * FirstThirdBaseStateを生成する。
     *
     * @param context 所属するイニング状態
     * @return 指定された試合に所属するState
     */
    public FirstThirdBaseState createFirstThirdBaseState(InningStateContext context) {
        return new FirstThirdBaseState(context);
    }

    /**
     * DoubleThirdBaseStateを生成する。
     *
     * @param context 所属するイニング状態
     * @return 指定された試合に所属するState
     */
    public DoubleThirdBaseState createDoubleThirdBaseState(InningStateContext context) {
        return new DoubleThirdBaseState(context);
    }

    /**
     * FullBasesStateを生成する。
     *
     * @param context 所属するイニング状態
     * @return 指定された試合に所属するState
     */
    public FullBasesState createFullBasesState(InningStateContext context) {
        return new FullBasesState(context);
    }
}
