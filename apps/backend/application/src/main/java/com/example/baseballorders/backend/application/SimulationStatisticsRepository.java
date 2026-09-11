package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.domain.SimulationStatistics;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 認証済みユーザー向け統計情報を永続化するポート。 */
public interface SimulationStatisticsRepository {
    /** 統計情報を保存する。 */
    SimulationStatistics save(SimulationStatistics statistics);

    /** ユーザーの統計情報を取得する。 */
    List<SimulationStatistics> findAllByUserId(Long userId);

    /** ユーザー所有の特定統計情報を取得する。 */
    Optional<SimulationStatistics> findBySimulationIdAndUserId(UUID simulationId, Long userId);
}
