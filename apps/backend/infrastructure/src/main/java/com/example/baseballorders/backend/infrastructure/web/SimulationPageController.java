package com.example.baseballorders.backend.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/** 画面入力で打順設定を行うシミュレーション画面を提供する。 */
@Controller
public final class SimulationPageController {

    /**
     * 空の打順入力欄を含む1試合実行画面を表示する。
     *
     * @return 1試合実行画面
     */
    @GetMapping("/")
    public ModelAndView index() {
        return new ModelAndView("single-game");
    }

    /**
     * 空の打順入力欄を含む大規模実行画面を表示する。
     *
     * @return 大規模実行画面
     */
    @GetMapping("/large-scale")
    public ModelAndView largeScale() {
        return new ModelAndView("simulation");
    }
}
