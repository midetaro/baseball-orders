package com.example.baseballorders.backend.infrastructure.api;

import com.example.baseballorders.backend.application.SimulationStatisticsRepository;
import com.example.baseballorders.backend.domain.SimulationStatistics;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ログイン済みユーザーのシミュレーション統計情報を返すController。 */
@RestController
@RequestMapping("/simulation-statistics")
@RequiredArgsConstructor
public final class SimulationStatisticsController {
    private final SimulationStatisticsRepository repository;

    /** ログイン済みユーザー自身の統計情報一覧を返す。 */
    @GetMapping
    public List<SimulationStatistics> list() {
        return repository.findAllByUserId(authenticatedUserId());
    }

    /** ログイン済みユーザー自身が所有する統計情報を返す。 */
    @GetMapping("/{simulationId}")
    public SimulationStatistics find(@PathVariable UUID simulationId) {
        return repository
                .findBySimulationIdAndUserId(simulationId, authenticatedUserId())
                .orElseThrow(StatisticsNotFoundException::new);
    }

    private static Long authenticatedUserId() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long userId) return userId;
        throw new IllegalStateException("authenticated user ID is missing");
    }

    /** 所有する統計情報がないことを表すHTTP例外。 */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    static final class StatisticsNotFoundException extends RuntimeException {}
}
