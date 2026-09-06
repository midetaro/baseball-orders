#!/usr/bin/env python3
"""ComposeがSimulatorのapplication.ymlの試合数を上書きしないことを検証する。"""

import json
from pathlib import Path
import subprocess
import sys
import unittest


class SimulatorComposeConfigurationTest(unittest.TestCase):
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


if __name__ == "__main__":
    if len(sys.argv) == 1:
        sys.argv.append(str(Path(__file__).resolve().parents[2] / "compose.yaml"))
    unittest.main(argv=[sys.argv[0]])
