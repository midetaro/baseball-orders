package com.example.baseballorders.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** gRPCでシミュレーション要求を受け付けるSimulatorの単独起動エントリーポイント。 */
@SpringBootApplication
public class SimulatorApplication {

    /**
     * Simulatorを起動し、gRPCサーバーを起動して要求の受付を開始する。
     *
     * @param args Spring Bootの起動引数
     */
    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }
}
