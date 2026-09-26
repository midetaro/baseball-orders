package com.example.baseballorders.simulator.application.usecase;

/** 1回のシミュレーション要求で実行する試合の種別。 */
public enum SimulationRunMode {

    /** 設定された試合数を繰り返し実行し、集計統計だけを返す大規模実行。 */
    LARGE_SCALE_RUN,

    /** 1試合だけを実行し、集計統計に加えてプレーごとの状況推移を返す1試合実行。 */
    SINGLE_GAME_RUN
}
