package com.example.baseballorders.backend.infrastructure.messaging;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** simulatorのgRPCサーバーへ接続するチャネルを設定ファイルの接続先から構成する。 */
@Configuration
public class SimulatorGrpcChannelConfiguration {

    private final String host;
    private final int port;

    /**
     * 設定ファイルのsimulator接続先を受け取って構成を作成する。
     *
     * @param host simulatorのgRPCサーバーのホスト名
     * @param port simulatorのgRPCサーバーのポート番号
     */
    public SimulatorGrpcChannelConfiguration(
            @Value("${baseball-orders.simulator.grpc.host}") String host,
            @Value("${baseball-orders.simulator.grpc.port}") int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * simulatorへ平文で接続するチャネルを生成する。接続は最初のRPCで確立され、アプリケーション終了時に閉じられる。
     *
     * @return simulatorのgRPCサーバーへ接続するチャネル
     */
    @Bean(destroyMethod = "shutdown")
    public ManagedChannel simulatorChannel() {
        return Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create())
                .build();
    }
}
