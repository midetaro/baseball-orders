package com.example.baseballorders.simulator.application.contract;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationRequestTest {

    @Test
    @DisplayName("シミュレーション要求をJSONに変換するとsimulation_idが出力される")
    void serializesSimulationIdAsSnakeCase() throws Exception {
        // given
        ObjectMapper objectMapper = new ObjectMapper();
        var request = new SimulationRequest("simulation-1", "game-1", "result-url", List.of());

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        // then
        assertAll(
                () -> assertEquals("simulation-1", json.get("simulation_id").textValue()),
                () -> assertEquals(null, json.get("simulationId")));
    }
}
