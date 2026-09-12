package com.example.baseballorders.backend.infrastructure.web;

import com.example.baseballorders.backend.application.adapter.PlayerListQuery;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/** 打者一覧と打順設定を行うシミュレーション画面を提供する。 */
@Controller
@RequiredArgsConstructor
public final class SimulationPageController {

    @NonNull private final PlayerListQuery playerListQuery;

    /**
     * 打者一覧を含むシミュレーション画面を表示する。
     *
     * @return 打者一覧を保持するシミュレーション画面
     */
    @GetMapping("/")
    public ModelAndView index() {
        var players = playerListQuery.findAll();
        return new ModelAndView("simulation", "players", players);
    }
}
