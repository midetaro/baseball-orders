package com.example.baseballorders.backend.infrastructure.persistence;

import com.example.baseballorders.backend.application.adapter.DefaultLineupRepository;
import com.example.baseballorders.backend.domain.DefaultBatter;
import com.example.baseballorders.backend.domain.DefaultBatterBuilder;
import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.example.baseballorders.backend.domain.TeamDefaultLineup;
import com.example.baseballorders.backend.domain.TeamDefaultLineupBuilder;
import com.example.baseballorders.backend.domain.TeamStrength;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** H2のteam_default_batterテーブルから既定オーダーを読み出すJDBCアダプタ。 */
@Repository
public class JdbcDefaultLineupRepository implements DefaultLineupRepository {

    private static final String SELECT_ALL =
            "SELECT team, batting_order, hit_average, personality, steal_forced, bunt_forced "
                    + "FROM team_default_batter ORDER BY team, batting_order";

    private final JdbcClient jdbcClient;

    /**
     * JDBCクライアントを指定してアダプタを生成する。
     *
     * @param jdbcClient H2へ問い合わせるJDBCクライアント
     */
    public JdbcDefaultLineupRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<TeamDefaultLineup> findAll() {
        Map<TeamStrength, List<DefaultBatter>> battersByTeam = new EnumMap<>(TeamStrength.class);
        jdbcClient
                .sql(SELECT_ALL)
                .query(
                        (resultSet, _) -> {
                            TeamStrength team = TeamStrength.valueOf(resultSet.getString("team"));
                            DefaultBatter batter =
                                    DefaultBatterBuilder.defaultBatter()
                                            .battingOrder(resultSet.getInt("batting_order"))
                                            .hitAverage(
                                                    resultSet
                                                            .getBigDecimal("hit_average")
                                                            .floatValue())
                                            .personality(
                                                    PlayerPersonality.valueOf(
                                                            resultSet.getString("personality")))
                                            .stealForced(resultSet.getBoolean("steal_forced"))
                                            .buntForced(resultSet.getBoolean("bunt_forced"))
                                            .build();
                            battersByTeam.computeIfAbsent(team, _ -> new ArrayList<>()).add(batter);
                            return batter;
                        })
                .list();
        List<TeamDefaultLineup> lineups = new ArrayList<>();
        for (TeamStrength team : TeamStrength.values()) {
            List<DefaultBatter> batters = battersByTeam.get(team);
            if (batters != null) {
                lineups.add(
                        TeamDefaultLineupBuilder.teamDefaultLineup()
                                .team(team)
                                .batters(batters)
                                .build());
            }
        }
        return List.copyOf(lineups);
    }
}
