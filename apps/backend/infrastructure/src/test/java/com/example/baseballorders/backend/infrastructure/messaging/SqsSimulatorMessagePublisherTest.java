package com.example.baseballorders.backend.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.domain.PitcherPersonality;
import com.example.baseballorders.backend.domain.PlayerDataBuilder;
import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SqsSimulatorMessagePublisherTest {

    @Test
    @DisplayName("共有要求contractをsimulation-requestキューへ送信する")
    void sendsRequestToSimulationRequestQueue() {
        // given
        // SqsTemplateをモックする
        SqsTemplate sqsTemplate = mock(SqsTemplate.class);
        var publisher = new SqsSimulatorMessagePublisher(sqsTemplate, "test-request-queue");
        var request = new SimulationRequestMessage(UUID.randomUUID(), "1", List.of());

        // when
        publisher.publish(request);

        // then
        assertAll(() -> verify(sqsTemplate).send("test-request-queue", request));
    }

    @Test
    @DisplayName("backendの選手データを性格を含む共有要求へ変換する")
    void mapsSuccessRatesToSharedRequest() {
        // given
        SqsTemplate sqsTemplate = mock(SqsTemplate.class);
        var publisher = new SqsSimulatorMessagePublisher(sqsTemplate, "test-request-queue");
        var request =
                new SimulationRequest(
                        UUID.randomUUID(),
                        "1",
                        List.of(
                                PlayerDataBuilder.playerData()
                                        .name("選手1")
                                        .hitAverage(0.321f)
                                        .sluggish(0.400f)
                                        .buntSuccessRate(0.700f)
                                        .buntEnabled(true)
                                        .stealSuccessRate(0.678f)
                                        .stealEnabled(true)
                                        .personality(PlayerPersonality.EAGER_BUNT)
                                        .build()));
        var messageCaptor = ArgumentCaptor.forClass(SimulationRequestMessage.class);

        // when
        publisher.publish(request);

        // then
        verify(sqsTemplate).send(eq("test-request-queue"), messageCaptor.capture());
        assertAll(
                () ->
                        assertEquals(
                                0.700f,
                                messageCaptor.getValue().players().getFirst().buntSuccessRate()),
                () ->
                        assertEquals(
                                true, messageCaptor.getValue().players().getFirst().buntEnabled()),
                () ->
                        assertEquals(
                                0.678f,
                                messageCaptor.getValue().players().getFirst().stealSuccessRate()),
                () ->
                        assertEquals(
                                com.example.baseballorders.messaging.PlayerPersonality.EAGER_BUNT,
                                messageCaptor.getValue().players().getFirst().personality()),
                () ->
                        assertEquals(
                                com.example.baseballorders.messaging.PitcherPersonality.DEFAULT,
                                messageCaptor.getValue().pitcherPersonality()));
    }

    @Test
    @DisplayName("投手性格を共有要求へ変換する")
    void mapsPitcherPersonalityToSharedRequest() {
        // given
        SqsTemplate sqsTemplate = mock(SqsTemplate.class);
        var publisher = new SqsSimulatorMessagePublisher(sqsTemplate, "test-request-queue");
        var request =
                new SimulationRequest(
                        UUID.randomUUID(), "1", List.of(), PitcherPersonality.TECHNICAL);
        var messageCaptor = ArgumentCaptor.forClass(SimulationRequestMessage.class);

        // when
        publisher.publish(request);

        // then
        verify(sqsTemplate).send(eq("test-request-queue"), messageCaptor.capture());
        assertAll(
                () ->
                        assertEquals(
                                com.example.baseballorders.messaging.PitcherPersonality.TECHNICAL,
                                messageCaptor.getValue().pitcherPersonality()));
    }
}
