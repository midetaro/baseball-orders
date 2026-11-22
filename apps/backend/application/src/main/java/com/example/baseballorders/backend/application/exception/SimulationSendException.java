package com.example.baseballorders.backend.application.exception;

import java.util.UUID;

/** simulatorへのシミュレーション要求が失敗したこと(到達不能またはエラー応答)を表す。 */
public final class SimulationSendException extends RuntimeException {

    /**
     * 送信失敗の相関IDと原因を保持する例外を作成する。
     *
     * @param simulationId 送信できなかった相関ID
     * @param cause simulator呼び出し時の原因
     */
    public SimulationSendException(UUID simulationId, RuntimeException cause) {
        super("simulation request could not be sent: " + simulationId, cause);
    }
}
