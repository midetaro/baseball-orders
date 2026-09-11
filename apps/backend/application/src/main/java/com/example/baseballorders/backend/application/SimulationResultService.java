package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.domain.SimulationResult;
import com.example.baseballorders.backend.domain.SimulationStatistics;
import com.example.baseballorders.backend.domain.SimulationStatus;
import java.time.Clock;
import java.util.UUID;

/** 結果受信時の統計保存とシミュレーション完了を調整する。 */
public final class SimulationResultService {
    private final SimulationRepository simulationRepository;
    private final SimulationStatisticsRepository statisticsRepository;
    private final Clock clock;

    /** 結果保存サービスを作成する。 */
    public SimulationResultService(
            SimulationRepository simulationRepository,
            SimulationStatisticsRepository statisticsRepository,
            Clock clock) {
        this.simulationRepository = simulationRepository;
        this.statisticsRepository = statisticsRepository;
        this.clock = clock;
    }

    /** 結果を一度だけ保存してシミュレーションを完了する。 */
    public void saveSimulationResult(UUID simulationId, SimulationResult result) {
        var simulation = simulationRepository.findBySimulationId(simulationId);
        if (simulation.isEmpty()) return;
        if (simulation.get().status() == SimulationStatus.COMPLETED) return;
        if (simulation.get().userId() != null)
            statisticsRepository.save(
                    SimulationStatistics.create(
                            simulation.get().userId(), result, clock.instant()));
        simulationRepository.save(simulation.get().complete(clock.instant()));
    }
}
