package com.example.baseballorders.backend.application.adapter;

import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.application.exception.SimulationSendException;
import com.example.baseballorders.backend.application.exception.SimulationTimeoutException;
import com.example.baseballorders.backend.domain.SimulationResult;

/** simulatorへシミュレーション要求を同期的に送り、その結果を受け取るポート。 */
@FunctionalInterface
public interface SimulatorClient {

    /**
     * 指定された打順でsimulatorにシミュレーションを要求し、応答が届くまで待機して結果を返す。
     *
     * @param request シミュレーションIDと9人の選手データを含む要求
     * @return simulatorから受け取ったシミュレーション結果
     * @throws SimulationTimeoutException 許容時間内にsimulatorが応答しなかった場合
     * @throws SimulationSendException simulatorへ到達できない場合、またはsimulatorがエラーを返した場合
     */
    SimulationResult simulate(SimulationRequest request);
}
