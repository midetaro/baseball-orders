package com.example.baseballorders.backend.application.adapter;

import com.example.baseballorders.backend.domain.TeamDefaultLineup;
import java.util.List;

/** チームごとの既定オーダーを取得するポート。 */
public interface DefaultLineupRepository {

    /**
     * 全チームの既定オーダーを取得する。
     *
     * @return TeamStrengthの宣言順に並んだ既定オーダー。各打者は打順順
     */
    List<TeamDefaultLineup> findAll();
}
