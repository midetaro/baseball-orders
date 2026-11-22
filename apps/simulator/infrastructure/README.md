# Simulator のローカル起動

Backend・SimulatorをまとめてDockerで動かす場合は、
[ローカルDocker環境](../../../infra/docker/README.md)を参照してください。

Simulator は gRPC サーバー（`SimulationService/Simulate`）として起動し、既定では `9090` 番ポートで待ち受けます。
ポートは `SIMULATION_GRPC_PORT`（`simulation.grpc.port`）で変更できます。

gRPC アダプタ（`GrpcSimulationService`・`GrpcServerLifecycle`）の単体テストは外部サービスを必要とせず、
通常の `./gradlew :infrastructure:test` で実行されます。Backend との疎通は `integration-test` が検証します。
