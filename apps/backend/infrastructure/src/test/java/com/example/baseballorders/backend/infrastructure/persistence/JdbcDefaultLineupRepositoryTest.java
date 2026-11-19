package com.example.baseballorders.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.backend.domain.DefaultBatter;
import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.example.baseballorders.backend.domain.TeamDefaultLineup;
import com.example.baseballorders.backend.domain.TeamStrength;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.cloud.aws.sqs.enabled=false")
class JdbcDefaultLineupRepositoryTest {

    @MockitoBean private SqsTemplate sqsTemplate;

    @Autowired private JdbcDefaultLineupRepository sut;

    private static final PlayerPersonality STEAL = PlayerPersonality.EAGER_STEAL;
    private static final PlayerPersonality BUNT = PlayerPersonality.EAGER_BUNT;
    private static final PlayerPersonality MID = PlayerPersonality.MIDDLE_DISTANCE;
    private static final PlayerPersonality SLUG = PlayerPersonality.EAGER_SLUGGISH;
    private static final PlayerPersonality BASE = PlayerPersonality.HIGH_ON_BASE;
    private static final PlayerPersonality DEF = PlayerPersonality.DEFAULT;

    private static String describe(TeamDefaultLineup lineup) {
        StringBuilder text = new StringBuilder();
        for (DefaultBatter batter : lineup.batters()) {
            text.append(batter.battingOrder())
                    .append(',')
                    .append(batter.hitAverage())
                    .append(',')
                    .append(batter.personality())
                    .append(',')
                    .append(batter.stealForced())
                    .append(',')
                    .append(batter.buntForced())
                    .append('/');
        }
        return text.toString();
    }

    private static String expected(float[] averages, PlayerPersonality[] personalities) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < averages.length; index++) {
            text.append(index + 1)
                    .append(',')
                    .append(averages[index])
                    .append(',')
                    .append(personalities[index])
                    .append(',')
                    .append(index == 0)
                    .append(',')
                    .append(index == 1)
                    .append('/');
        }
        return text.toString();
    }

    @Test
    @DisplayName("強・並・弱の3チームの既定オーダーを順序どおりに取得できる")
    void findsAllTeamsWithDefaultData() {
        // given
        // when
        List<TeamDefaultLineup> lineups = sut.findAll();

        // then
        assertAll(
                () ->
                        assertEquals(
                                List.of(
                                        TeamStrength.STRONG,
                                        TeamStrength.AVERAGE,
                                        TeamStrength.WEAK),
                                lineups.stream().map(TeamDefaultLineup::team).toList()),
                () ->
                        assertEquals(
                                expected(
                                        new float[] {
                                            .300f, .265f, .330f, .280f, .275f, .280f, .250f, .275f,
                                            .280f
                                        },
                                        new PlayerPersonality[] {
                                            STEAL, BUNT, MID, SLUG, MID, DEF, MID, MID, BASE
                                        }),
                                describe(lineups.get(0))),
                () ->
                        assertEquals(
                                expected(
                                        new float[] {
                                            .280f, .260f, .300f, .270f, .250f, .250f, .250f, .240f,
                                            .270f
                                        },
                                        new PlayerPersonality[] {
                                            STEAL, DEF, MID, SLUG, MID, DEF, DEF, DEF, BASE
                                        }),
                                describe(lineups.get(1))),
                () ->
                        assertEquals(
                                expected(
                                        new float[] {
                                            .270f, .250f, .280f, .250f, .250f, .230f, .230f, .230f,
                                            .260f
                                        },
                                        new PlayerPersonality[] {
                                            STEAL, DEF, MID, MID, DEF, DEF, DEF, DEF, BASE
                                        }),
                                describe(lineups.get(2))));
    }
}
