package com.example.baseballorders.backend.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** シミュレーションロジックを説明するページを提供する。 */
@Controller
public final class SimulationGuidePageController {

    /**
     * シミュレーションの入力、試合進行、結果集計を説明するページを表示する。
     *
     * @return シミュレーションの仕組みを説明する画面
     */
    @GetMapping("/simulation-guide")
    public String index() {
        return "simulation-guide";
    }
}
