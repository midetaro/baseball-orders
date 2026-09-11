package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.domain.Simulation;
import java.util.Optional;
import java.util.UUID;

/** シミュレーションの要求状態を永続化するポート。 */
public interface SimulationRepository {
    /** 新しいシミュレーションを保存する。 */
    Simulation save(Simulation simulation);

    /** 相関IDでシミュレーションを取得する。 */
    Optional<Simulation> findBySimulationId(UUID simulationId);
}
