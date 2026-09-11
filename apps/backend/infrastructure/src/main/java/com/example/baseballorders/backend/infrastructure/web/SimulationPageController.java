package com.example.baseballorders.backend.infrastructure.web;

import com.example.baseballorders.backend.infrastructure.persistence.PlayerEntity;
import jakarta.persistence.EntityManager;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/** 打者一覧と打順設定を行うシミュレーション画面を提供する。 */
@Controller
@RequiredArgsConstructor
public final class SimulationPageController {

    @NonNull private final EntityManager entityManager;

    /**
     * player ID順の打者一覧とログイン状態を含むシミュレーション画面を表示する。
     *
     * @param authentication 現在のリクエストに関連付く認証情報。匿名アクセス時は{@code null}または匿名認証
     * @return 打者一覧を保持するシミュレーション画面
     */
    @GetMapping("/")
    public ModelAndView index(Authentication authentication) {
        var players =
                entityManager
                        .createQuery(
                                "SELECT p FROM PlayerEntity p ORDER BY p.playerId",
                                PlayerEntity.class)
                        .getResultList();
        var loggedIn =
                authentication != null
                        && authentication.isAuthenticated()
                        && authentication.getPrincipal() instanceof Long;
        var page = new ModelAndView("simulation", "players", players);
        page.addObject("loggedIn", loggedIn);
        return page;
    }
}
