#!/usr/bin/env python3
"""ローカルCompose設定を検証する。"""

import json
import os
from pathlib import Path
import subprocess
import sys
import unittest


class ComposeConfigurationTest(unittest.TestCase):
    def test_preserves_application_game_count(self):
        # given
        compose_file = Path(sys.argv[1]).resolve()
        # when
        configuration = json.loads(subprocess.check_output(
            ["docker", "compose", "-f", str(compose_file), "config", "--format", "json"],
            text=True,
        ))
        # then
        self.assertNotIn(
            "SIMULATION_GAME_COUNT",
            configuration["services"]["simulator"]["environment"],
            "application.ymlのsimulation.game-countをComposeで上書きしない",
        )

    def test_wires_backend_to_simulator_over_grpc_without_queues(self):
        # given
        compose_file = Path(sys.argv[1]).resolve()
        # when
        configuration = json.loads(subprocess.check_output(
            ["docker", "compose", "-f", str(compose_file), "config", "--format", "json"],
            text=True,
        ))
        # then
        services = configuration["services"]
        self.assertEqual({"backend", "simulator"}, set(services))
        self.assertEqual("simulator", services["backend"]["environment"]["SIMULATOR_GRPC_HOST"])
        self.assertEqual("9090", services["backend"]["environment"]["SIMULATOR_GRPC_PORT"])
        self.assertEqual("9090", services["simulator"]["environment"]["SIMULATION_GRPC_PORT"])
        self.assertIn("simulator", services["backend"]["depends_on"])
        self.assertEqual(
            ["9090"],
            [str(port["target"]) for port in services["simulator"]["expose"]]
            if services["simulator"].get("expose") and isinstance(services["simulator"]["expose"][0], dict)
            else [str(port) for port in services["simulator"]["expose"]],
        )
        self.assertNotIn("ports", services["simulator"], "Simulatorはホストへ公開しない")
        for service in services.values():
            self.assertFalse(
                [name for name in service.get("environment", {}) if "SQS" in name.upper() or name.startswith("AWS_")],
                "メッセージキュー用の環境変数を渡さない",
            )

    def test_does_not_forward_google_oauth_credentials_to_backend(self):
        # given
        compose_files = (
            Path(sys.argv[1]).resolve(),
            Path(__file__).resolve().parents[2]
            / "apps/backend/infrastructure/compose-backend.yaml",
        )
        # when
        environment = {
            **os.environ,
            "GOOGLE_CLIENT_ID": "test-google-client-id",
            "GOOGLE_CLIENT_SECRET": "test-google-client-secret",
        }
        configurations = [
            json.loads(subprocess.check_output(
                ["docker", "compose", "-f", str(compose_file), "config", "--format", "json"],
                text=True,
                env=environment,
            ))
            for compose_file in compose_files
        ]
        # then
        for configuration in configurations:
            with self.subTest(compose_file=configuration["name"]):
                backend_environment = configuration["services"]["backend"]["environment"]
                self.assertNotIn(
                    "GOOGLE_CLIENT_ID",
                    backend_environment,
                    "ログイン機能がないためBackendへGoogle OAuthのclient IDを渡さない",
                )
                self.assertNotIn(
                    "GOOGLE_CLIENT_SECRET",
                    backend_environment,
                    "ログイン機能がないためBackendへGoogle OAuthのclient secretを渡さない",
                )

if __name__ == "__main__":
    if len(sys.argv) == 1:
        sys.argv.append(str(Path(__file__).resolve().parents[2] / "compose.yaml"))
    unittest.main(argv=[sys.argv[0]])
