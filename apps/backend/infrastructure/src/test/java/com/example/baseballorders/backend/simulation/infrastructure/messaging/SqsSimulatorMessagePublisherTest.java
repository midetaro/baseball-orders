package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.baseballorders.messaging.SimulationRequestMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqsSimulatorMessagePublisherTest {

    @Test
    @DisplayName("共有要求contractをsimulation-requestキューへ送信する")
    void sendsRequestToSimulationRequestQueue() {
        // given
        SqsTemplate sqsTemplate = mock(SqsTemplate.class);
        var publisher = new SqsSimulatorMessagePublisher(sqsTemplate);
        var request = new SimulationRequestMessage(UUID.randomUUID(), "1", List.of());

        // when
        publisher.publish(request);

        // then
        assertAll(() -> verify(sqsTemplate).send("simulation-request", request));
    }
}
