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

    def test_forwards_google_oauth_credentials_to_backend(self):
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
                self.assertEqual(
                    "test-google-client-id",
                    configuration["services"]["backend"]["environment"]["GOOGLE_CLIENT_ID"],
                    "BackendへGoogle OAuthのclient IDを引き継ぐ",
                )
                self.assertEqual(
                    "test-google-client-secret",
                    configuration["services"]["backend"]["environment"]["GOOGLE_CLIENT_SECRET"],
                    "BackendへGoogle OAuthのclient secretを引き継ぐ",
                )


if __name__ == "__main__":
    if len(sys.argv) == 1:
        sys.argv.append(str(Path(__file__).resolve().parents[2] / "compose.yaml"))
    unittest.main(argv=[sys.argv[0]])
