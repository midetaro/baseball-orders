package com.example.baseballorders.backend.domain;

import java.util.List;
import java.util.Objects;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** チームごとの既定オーダー。打者は打順どおり9人で構成される。 */
@Builder(style = BuilderStyle.STAGED)
public record TeamDefaultLineup(TeamStrength team, List<DefaultBatter> batters) {

    // 打順は9人という野球の固定ルール。
    private static final int BATTER_COUNT = 9;

    /**
     * チームと打者を検証し、打者リストを不変コピーする。
     *
     * @throws NullPointerException teamまたはbattersがnullの場合
     * @throws IllegalArgumentException 打者が9人でない、または打順どおりでない場合
     */
    public TeamDefaultLineup {
        Objects.requireNonNull(team, "team must not be null");
        Objects.requireNonNull(batters, "batters must not be null");
        batters = List.copyOf(batters);
        if (batters.size() != BATTER_COUNT) {
            throw new IllegalArgumentException("batters must contain exactly 9 batters");
        }
        for (int index = 0; index < batters.size(); index++) {
            if (batters.get(index).battingOrder() != index + 1) {
                throw new IllegalArgumentException("batters must be in batting order");
            }
        }
    }
}
