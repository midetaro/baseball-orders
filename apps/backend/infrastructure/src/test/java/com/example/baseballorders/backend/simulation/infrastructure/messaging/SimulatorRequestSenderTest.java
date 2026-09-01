package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulatorRequestSenderTest {

    @Test
    @DisplayName("メッセージ発行処理がnullの場合は送信機能を作成できない")
    void rejectsNullPublisher() {
        // given

        // when
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new SimulatorRequestSender(null));

        // then
        assertAll(() -> assertEquals("publisher must not be null", exception.getMessage()));
    }

    @Test
    @DisplayName("APIから9人の選手データを受け取るとsimulatorへメッセージを送信する")
    void sendsNinePlayersToSimulator() {
        // given
        var publishedMessages = new ArrayList<List<PlayerData>>();
        var sender = new SimulatorRequestSender(publishedMessages::add);
        var players = players(9);

        // when
        sender.send(players);

        // then
        assertAll(
                () -> assertEquals(1, publishedMessages.size()),
                () -> assertEquals(players, publishedMessages.getFirst()),
                () -> assertEquals("player-1", publishedMessages.getFirst().getFirst().name()),
                () -> assertEquals(0.301f, publishedMessages.getFirst().getFirst().hitAverage()),
                () -> assertEquals(0.401f, publishedMessages.getFirst().getFirst().sluggish()));
    }

    @Test
    @DisplayName("APIから8人の選手データを受け取るとメッセージを送信せず拒否する")
    void rejectsFewerThanNinePlayers() {
        // given
        var publishedMessages = new ArrayList<List<PlayerData>>();
        var sender = new SimulatorRequestSender(publishedMessages::add);

        // when
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> sender.send(players(8)));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "players must contain exactly 9 entries", exception.getMessage()),
                () -> assertEquals(0, publishedMessages.size()));
    }

    @Test
    @DisplayName("APIから10人の選手データを受け取るとメッセージを送信せず拒否する")
    void rejectsMoreThanNinePlayers() {
        // given
        var publishedMessages = new ArrayList<List<PlayerData>>();
        var sender = new SimulatorRequestSender(publishedMessages::add);

        // when
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> sender.send(players(10)));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "players must contain exactly 9 entries", exception.getMessage()),
                () -> assertEquals(0, publishedMessages.size()));
    }

    @Test
    @DisplayName("APIからnullの選手データを受け取るとメッセージを送信せず拒否する")
    void rejectsNullPlayers() {
        // given
        var publishedMessages = new ArrayList<List<PlayerData>>();
        var sender = new SimulatorRequestSender(publishedMessages::add);

        // when
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> sender.send(null));

        // then
        assertAll(
                () -> assertEquals("players must not be null", exception.getMessage()),
                () -> assertEquals(0, publishedMessages.size()));
    }

    @Test
    @DisplayName("9人の選手データにnullが含まれるとメッセージを送信せず拒否する")
    void rejectsNullPlayer() {
        // given
        var publishedMessages = new ArrayList<List<PlayerData>>();
        var sender = new SimulatorRequestSender(publishedMessages::add);
        var players = new ArrayList<>(players(9));
        players.set(8, null);

        // when
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> sender.send(players));

        // then
        assertAll(
                () -> assertEquals(0, publishedMessages.size()),
                () -> assertEquals(NullPointerException.class, exception.getClass()));
    }

    @Test
    @DisplayName("送信後にAPI側のリストを変更しても送信メッセージは変更されない")
    void publishesSnapshotOfPlayers() {
        // given
        var publishedMessages = new ArrayList<List<PlayerData>>();
        var sender = new SimulatorRequestSender(publishedMessages::add);
        var players = new ArrayList<>(players(9));

        // when
        sender.send(players);
        players.clear();

        // then
        assertAll(() -> assertEquals(9, publishedMessages.getFirst().size()));
    }

    private static List<PlayerData> players(int size) {
        return java.util.stream.IntStream.rangeClosed(1, size)
                .mapToObj(
                        number ->
                                new PlayerData(
                                        "player-" + number,
                                        0.3f + number / 1000f,
                                        0.4f + number / 1000f))
                .toList();
    }
}
