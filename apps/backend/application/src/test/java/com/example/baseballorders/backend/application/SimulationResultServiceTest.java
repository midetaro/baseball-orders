package com.example.baseballorders.backend.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.backend.domain.Simulation;
import com.example.baseballorders.backend.domain.SimulationResult;
import com.example.baseballorders.backend.domain.SimulationStatistics;
import com.example.baseballorders.backend.domain.SimulationStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationResultServiceTest {
    @Test
    @DisplayName("未認証のシミュレーション結果は完了するが統計情報を保存しない")
    void completesAnonymousSimulationWithoutStatistics() {
        // given
        var simulations = new InMemorySimulationRepository();
        var statistics = new InMemoryStatisticsRepository();
        UUID simulationId = UUID.randomUUID();
        simulations.save(
                new Simulation(simulationId, null, SimulationStatus.PENDING, Instant.EPOCH, null));
        var service =
                new SimulationResultService(
                        simulations,
                        statistics,
                        Clock.fixed(Instant.ofEpochSecond(1), ZoneOffset.UTC));

        // when
        service.saveSimulationResult(simulationId, result(simulationId));

        // then
        assertAll(
                () ->
                        assertEquals(
                                SimulationStatus.COMPLETED,
                                simulations
                                        .findBySimulationId(simulationId)
                                        .orElseThrow()
                                        .status()),
                () -> assertEquals(0, statistics.saved.size()));
    }

    @Test
    @DisplayName("認証済みの結果は統計情報を一度だけ保存する")
    void savesAuthenticatedStatisticsOnlyOnce() {
        // given
        var simulations = new InMemorySimulationRepository();
        var statistics = new InMemoryStatisticsRepository();
        UUID simulationId = UUID.randomUUID();
        simulations.save(
                new Simulation(simulationId, 42L, SimulationStatus.PENDING, Instant.EPOCH, null));
        var service =
                new SimulationResultService(
                        simulations,
                        statistics,
                        Clock.fixed(Instant.ofEpochSecond(1), ZoneOffset.UTC));

        // when
        service.saveSimulationResult(simulationId, result(simulationId));
        service.saveSimulationResult(simulationId, result(simulationId));

        // then
        assertAll(
                () -> assertEquals(1, statistics.saved.size()),
                () -> assertEquals(42L, statistics.saved.getFirst().userId()),
                () -> assertEquals(6.5, statistics.saved.getFirst().averageScore()));
    }

    private static SimulationResult result(UUID simulationId) {
        return new SimulationResult(
                simulationId,
                List.of(new SimulationResult.Result(7, 3)),
                new SimulationResult.Statistics(6.5, 6, 9));
    }

    private static final class InMemorySimulationRepository implements SimulationRepository {
        private final Map<UUID, Simulation> simulations = new HashMap<>();

        @Override
        public Simulation save(Simulation simulation) {
            simulations.put(simulation.simulationId(), simulation);
            return simulation;
        }

        @Override
        public Optional<Simulation> findBySimulationId(UUID simulationId) {
            return Optional.ofNullable(simulations.get(simulationId));
        }
    }

    private static final class InMemoryStatisticsRepository
            implements SimulationStatisticsRepository {
        private final List<SimulationStatistics> saved = new ArrayList<>();

        @Override
        public SimulationStatistics save(SimulationStatistics statistics) {
            saved.add(statistics);
            return statistics;
        }

        @Override
        public List<SimulationStatistics> findAllByUserId(Long userId) {
            return saved.stream().filter(statistics -> statistics.userId().equals(userId)).toList();
        }

        @Override
        public Optional<SimulationStatistics> findBySimulationIdAndUserId(
                UUID simulationId, Long userId) {
            return saved.stream()
                    .filter(
                            statistics ->
                                    statistics.simulationId().equals(simulationId)
                                            && statistics.userId().equals(userId))
                    .findFirst();
        }
    }
}
