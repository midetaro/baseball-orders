package com.example.baseballorders.simulator.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.infrastructure.config.SimulationInfrastructureConfiguration;
import com.example.baseballorders.simulator.infrastructure.messaging.GrpcServerLifecycle;
import com.example.baseballorders.simulator.infrastructure.messaging.GrpcSimulationService;
import com.example.baseballorders.simulator.infrastructure.messaging.LineUpMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InfrastructurePlacementTest {

    @Test
    @DisplayName("gRPC境界クラスとインフラ設定を責務別パッケージに配置する")
    void placesGrpcBoundaryTypesInInfrastructure() {
        // given
        var expectedConfigurationPackage =
                "com.example.baseballorders.simulator.infrastructure.config";
        var expectedMessagingPackage =
                "com.example.baseballorders.simulator.infrastructure.messaging";

        // when
        var configurationPackage = SimulationInfrastructureConfiguration.class.getPackageName();
        var mapperPackage = LineUpMapper.class.getPackageName();
        var servicePackage = GrpcSimulationService.class.getPackageName();
        var lifecyclePackage = GrpcServerLifecycle.class.getPackageName();

        // then
        assertAll(
                () -> assertEquals(expectedConfigurationPackage, configurationPackage),
                () -> assertEquals(expectedMessagingPackage, mapperPackage),
                () -> assertEquals(expectedMessagingPackage, servicePackage),
                () -> assertEquals(expectedMessagingPackage, lifecyclePackage));
    }
}
