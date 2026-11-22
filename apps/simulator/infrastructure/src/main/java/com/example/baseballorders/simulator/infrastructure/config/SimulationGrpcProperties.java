package com.example.baseballorders.simulator.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code simulation.grpc} 配下の、シミュレーションを公開するgRPCサーバーの設定。
 *
 * @param port gRPCサーバーが待ち受けるポート。0を指定すると空いているポートが割り当てられる
 * @param shutdownGracePeriod 停止時に処理中のRPCの完了を待つ最大時間
 */
@ConfigurationProperties(prefix = "simulation.grpc")
public record SimulationGrpcProperties(int port, Duration shutdownGracePeriod) {}
