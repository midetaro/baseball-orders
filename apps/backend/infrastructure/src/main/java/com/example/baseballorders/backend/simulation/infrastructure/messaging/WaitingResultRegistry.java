package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import com.example.baseballorders.backend.simulation.domain.SimulationResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/** HTTP要求とSQS結果をsimulation IDで相関するthread-safeな待機レジストリ。 */
@Component
public final class WaitingResultRegistry {

    private final ConcurrentMap<UUID, CompletableFuture<SimulationResult>> pending =
            new ConcurrentHashMap<>();

    /**
     * 指定されたsimulation IDの結果待機を登録する。
     *
     * @param simulationId 相関ID
     * @return 結果受信用Future
     * @throws IllegalStateException 同じIDが登録済みの場合
     */
    public CompletableFuture<SimulationResult> register(UUID simulationId) {
        var future = new CompletableFuture<SimulationResult>();
        if (pending.putIfAbsent(simulationId, future) != null) {
            throw new IllegalStateException("simulation already registered: " + simulationId);
        }
        return future;
    }

    /**
     * 対応する待機を削除して結果を完了する。
     *
     * @param simulationId 相関ID
     * @param result 受信結果
     * @return 待機が存在して完了できた場合true
     */
    public boolean complete(UUID simulationId, SimulationResult result) {
        var future = pending.remove(simulationId);
        return future != null && future.complete(result);
    }

    /**
     * 指定された待機を削除する。
     *
     * @param simulationId 相関ID
     */
    public void remove(UUID simulationId) {
        pending.remove(simulationId);
    }

    /**
     * 現在待機中の要求数を返す。
     *
     * @return 待機要求数
     */
    public int pendingCount() {
        return pending.size();
    }
}
