package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import com.example.baseballorders.backend.application.dto.SimulationRequestBuilder;
import com.example.baseballorders.backend.application.exception.SimulationAcceptException;
import com.example.baseballorders.backend.application.exception.SimulationSendException;
import com.example.baseballorders.backend.application.exception.SimulationTimeoutException;
import com.example.baseballorders.backend.domain.PitcherPersonality;
import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.SimulationResult;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** シミュレーションユースケースのデータ取得、要求送信、結果待機を調整する。 */
public final class SimulationCoordinator {

    private static final int LINEUP_SIZE = 9;
    private static final String MESSAGE_VERSION = "1";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final float MAXIMUM_AVERAGE_HIT_AVERAGE = 0.350f;
    private static final float MAXIMUM_AVERAGE_SLUGGISH = 0.400f;
    private static final float PITCHER_INCREASE_MULTIPLIER = 1.300f;

    private final SimulatorMessagePublisher publisher;
    private final WaitingResultRegistry registry;
    private final Duration timeout;

    /**
     * 仕様既定の30秒timeoutでCoordinatorを作成する。
     *
     * @param publisher SQS要求Publisher
     * @param registry HTTPとSQS結果の待機レジストリ
     */
    public SimulationCoordinator(
            SimulatorMessagePublisher publisher, WaitingResultRegistry registry) {
        this(publisher, registry, DEFAULT_TIMEOUT);
    }

    /**
     * 指定したtimeoutでCoordinatorを作成する。
     *
     * @param publisher シミュレーション要求の送信ポート
     * @param registry HTTPと結果を相関するレジストリ
     * @param timeout 結果を待機する時間
     */
    public SimulationCoordinator(
            SimulatorMessagePublisher publisher, WaitingResultRegistry registry, Duration timeout) {
        this.publisher = publisher;
        this.registry = registry;
        this.timeout = timeout;
    }

    /**
     * 画面入力された選手データをSQSへ要求し、相関する結果をtimeoutまで待機する。
     *
     * @param players 打順どおりの9人の入力済み選手データ
     * @return simulatorから受信した結果
     * @throws SimulationTimeoutException timeout内に結果を受信できなかった場合
     */
    public SimulationResult simulate(List<PlayerData> players) {
        return simulate(players, PitcherPersonality.DEFAULT);
    }

    /**
     * 画面入力された選手データと投手性格をSQSへ要求し、相関する結果をtimeoutまで待機する。
     *
     * @param players 打順どおりの9人の入力済み選手データ
     * @param pitcherPersonality 対戦する投手の性格
     * @return simulatorから受信した結果
     * @throws SimulationTimeoutException timeout内に結果を受信できなかった場合
     */
    public SimulationResult simulate(
            List<PlayerData> players, PitcherPersonality pitcherPersonality) {
        Objects.requireNonNull(pitcherPersonality, "pitcherPersonality must not be null");
        if (players.size() != LINEUP_SIZE) {
            throw new IllegalArgumentException("players must contain exactly 9 entries");
        }
        validateLineup(players, pitcherPersonality);
        UUID simulationId = UUID.randomUUID();

        // 送信
        // simulation-idの登録
        var waiting = registry.register(simulationId);
        try {
            // SQSの送信
            publisher.publish(
                    SimulationRequestBuilder.simulationRequest()
                            .simulationId(simulationId)
                            .version(MESSAGE_VERSION)
                            .players(players)
                            .pitcherPersonality(pitcherPersonality)
                            .build());
        } catch (RuntimeException exception) {
            registry.remove(simulationId);
            throw new SimulationSendException(simulationId, exception);
        }

        // 受信
        SimulationResult result;
        try {
            // simulation-idの取得
            result = waiting.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            throw new SimulationTimeoutException(simulationId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "simulation wait was interrupted: " + simulationId, exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("simulation result failed: " + simulationId, exception);
        } catch (RuntimeException exception) {
            throw new SimulationAcceptException(simulationId, exception);
        } finally {
            registry.remove(simulationId);
        }

        return result;
    }

    private static void validateLineup(
            List<PlayerData> players, PitcherPersonality pitcherPersonality) {
        double totalHitAverage = 0;
        double totalSluggish = 0;
        for (PlayerData player : players) {
            totalHitAverage += player.hitAverage();
            totalSluggish += player.sluggish();
            validatePitcherAdjustedProbabilities(player, pitcherPersonality);
        }
        if (totalHitAverage / LINEUP_SIZE > MAXIMUM_AVERAGE_HIT_AVERAGE) {
            throw new IllegalArgumentException("the average hitAverage must not exceed 0.35");
        }
        if (totalSluggish / LINEUP_SIZE > MAXIMUM_AVERAGE_SLUGGISH) {
            throw new IllegalArgumentException("the average sluggish must not exceed 0.4");
        }
    }

    private static void validatePitcherAdjustedProbabilities(
            PlayerData player, PitcherPersonality pitcherPersonality) {
        switch (pitcherPersonality) {
            case BOLD ->
                    requireProbability(
                            player.hitAverage() * PITCHER_INCREASE_MULTIPLIER, "hitAverage");
            case TECHNICAL ->
                    requireProbability(player.sluggish() * PITCHER_INCREASE_MULTIPLIER, "sluggish");
            case CAUTIOUS, DEFAULT -> {
                // These personalities do not increase batting probabilities beyond the input range.
            }
        }
    }

    private static void requireProbability(float value, String name) {
        if (value > 1.0f) {
            throw new IllegalArgumentException("pitcher-adjusted " + name + " must not exceed 1.0");
        }
    }
}
