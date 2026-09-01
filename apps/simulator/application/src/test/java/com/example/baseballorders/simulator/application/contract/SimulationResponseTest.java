package com.example.baseballorders.simulator.application.contract;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationResponseTest {

    @Test
    @DisplayName("シミュレーション応答をJSONに変換するとsimulation_idが出力される")
    void serializesSimulationIdAsSnakeCase() throws Exception {
        // given
        ObjectMapper objectMapper = new ObjectMapper();
        var response = new SimulationResponse("simulation-1", 5, 4);

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        // then
        assertAll(
                () -> assertEquals("simulation-1", json.get("simulation_id").textValue()),
                () -> assertEquals(null, json.get("simulationId")));
    }
}
