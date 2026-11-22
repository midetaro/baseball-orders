package com.example.baseballorders.simulator.infrastructure.messaging;

import com.example.baseballorders.simulator.infrastructure.config.SimulationGrpcProperties;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * シミュレーションのgRPCサーバーをSpringのライフサイクルに合わせて起動・停止する。
 *
 * <p>NettyのイベントループはデーモンスレッドのためWebサーバーを持たないSimulatorではJVMが終了してしまう。
 * そのためサーバーの終了を待つ非デーモンスレッドを置いてJVMを維持する。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GrpcServerLifecycle implements SmartLifecycle {

    private final SimulationGrpcProperties properties;
    private final GrpcSimulationService simulationService;

    private Server server;
    private Thread awaitThread;

    /** 設定されたポートでgRPCサーバーを起動し、JVMを維持する非デーモンスレッドを開始する。 */
    @Override
    public synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            server =
                    NettyServerBuilder.forPort(properties.port())
                            .addService(simulationService)
                            .build()
                            .start();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to start the gRPC server", exception);
        }
        log.info("gRPC server started port={}", server.getPort());
        Server started = server;
        awaitThread =
                new Thread(
                        () -> {
                            try {
                                started.awaitTermination();
                            } catch (InterruptedException _) {
                                Thread.currentThread().interrupt();
                            }
                        },
                        "grpc-server-await");
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    /** 新規RPCの受付を止め、猶予時間内に処理中のRPCの完了を待つ。完了しない場合は強制終了する。 */
    @Override
    public synchronized void stop() {
        if (server == null) {
            return;
        }
        server.shutdown();
        try {
            if (!server.awaitTermination(
                    properties.shutdownGracePeriod().toMillis(), TimeUnit.MILLISECONDS)) {
                log.warn("gRPC server did not terminate in the grace period; forcing shutdown");
                server.shutdownNow();
            }
            awaitThread.join();
        } catch (InterruptedException _) {
            server.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("gRPC server stopped");
        server = null;
        awaitThread = null;
    }

    /**
     * サーバーが起動中かどうかを返す。
     *
     * @return 起動済みで停止していない場合はtrue
     */
    @Override
    public synchronized boolean isRunning() {
        return server != null && !server.isTerminated();
    }

    /**
     * 実際に待ち受けているポートを返す。ポート0で起動した場合の割当先を取得するために使う。
     *
     * @return 待ち受けポート
     * @throws IllegalStateException サーバーが起動していない場合
     */
    public synchronized int getPort() {
        if (server == null) {
            throw new IllegalStateException("The gRPC server is not running");
        }
        return server.getPort();
    }
}
