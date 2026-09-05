package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.simulation.domain.PlayerData;
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
    @DisplayName("backendの選手データをバントと盗塁の成功率を含む共有要求へ変換する")
    void mapsSuccessRatesToSharedRequest() {
        // given
        SqsTemplate sqsTemplate = mock(SqsTemplate.class);
        var publisher = new SqsSimulatorMessagePublisher(sqsTemplate, "test-request-queue");
        var request =
                new SimulationRequest(
                        UUID.randomUUID(),
                        "1",
                        List.of(new PlayerData("選手1", 0.321f, 0.456f, 0.789f, 0.678f)));
        var messageCaptor = ArgumentCaptor.forClass(SimulationRequestMessage.class);

        // when
        publisher.publish(request);

        // then
        verify(sqsTemplate).send(eq("test-request-queue"), messageCaptor.capture());
        assertAll(
                () ->
                        assertEquals(
                                0.789f,
                                messageCaptor.getValue().players().getFirst().buntSuccessRate()),
                () ->
                        assertEquals(
                                0.678f,
                                messageCaptor.getValue().players().getFirst().stealSuccessRate()));
    }
}
