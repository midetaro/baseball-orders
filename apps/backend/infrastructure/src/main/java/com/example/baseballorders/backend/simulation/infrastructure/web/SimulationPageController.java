package com.example.baseballorders.backend.simulation.infrastructure.web;

import com.example.baseballorders.backend.simulation.infrastructure.persistence.PlayerEntity;
import jakarta.persistence.EntityManager;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/** 打者一覧と打順設定を行うシミュレーション画面を提供する。 */
@Controller
@RequiredArgsConstructor
public final class SimulationPageController {

    @NonNull private final EntityManager entityManager;

    /**
     * player ID順の打者一覧を含むシミュレーション画面を表示する。
     *
     * @return 打者一覧を保持するシミュレーション画面
     */
    @GetMapping("/")
    public ModelAndView index() {
        var players =
                entityManager
                        .createQuery(
                                "SELECT p FROM PlayerEntity p ORDER BY p.playerId",
                                PlayerEntity.class)
                        .getResultList();
        return new ModelAndView("simulation", "players", players);
    }
}
