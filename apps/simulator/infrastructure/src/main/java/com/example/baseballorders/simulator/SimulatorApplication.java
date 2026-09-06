package com.example.baseballorders.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** SQSを定期受信するSimulatorの単独起動エントリーポイント。 */
@SpringBootApplication
public class SimulatorApplication {

    /**
     * Simulatorを起動し、SQS要求の定期受信を開始する。
     *
     * @param args Spring Bootの起動引数
     */
    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }
}
