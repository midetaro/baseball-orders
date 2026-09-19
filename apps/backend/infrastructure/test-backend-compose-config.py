#!/usr/bin/env python3
"""Backend単体ComposeがBackendサービスだけを起動することを検証する。"""

import json
from pathlib import Path
import subprocess
import unittest


class BackendComposeConfigurationTest(unittest.TestCase):
    def test_runs_only_backend_with_the_local_profile(self):
        # given
        compose_file = Path(__file__).with_name("compose-backend.yaml")

        # when
        configuration = json.loads(
            subprocess.check_output(
                ["docker", "compose", "-f", str(compose_file), "config", "--format", "json"],
                text=True,
            )
        )

        # then
        self.assertEqual({"backend"}, set(configuration["services"]))
        self.assertEqual(
            "local", configuration["services"]["backend"]["environment"]["SPRING_PROFILES_ACTIVE"]
        )
        self.assertEqual(
            str(Path(__file__).resolve().parents[3]),
            configuration["services"]["backend"]["build"]["context"],
        )


if __name__ == "__main__":
    unittest.main()
