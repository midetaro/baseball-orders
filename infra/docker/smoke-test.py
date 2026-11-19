#!/usr/bin/env python3
"""実物: Docker上のBackend・Simulator・Flociと本番の試合計算。
モック: AWS SQSをFlociに置換。それ以外はなし。
担保する疎通: HTTP -> Backend -> 要求SQS -> 自動起動したSimulator
-> 結果SQS -> Backend -> HTTP応答。
担保しないもの: AWSのIAM・ネットワーク、乱数の統計的正当性、異常系・再配信。
"""

import json
import os
import urllib.request
import uuid


def main():
    base_url = "http://127.0.0.1:" + os.environ.get("BACKEND_PORT", "8080")
    with urllib.request.urlopen(base_url + "/", timeout=10) as response:
        assert response.status == 200

    player = {
        "hit_average": 0.33,
        "sluggish": 0.40,
        "bunt_enabled": True,
        "steal_enabled": True,
        "personality": "DEFAULT",
    }
    request = urllib.request.Request(
        base_url + "/simulations",
        data=json.dumps([player] * 9).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        assert response.status == 200
        result = json.load(response)
    uuid.UUID(result["simulationId"])
    statistics = result["statistics"]
    assert isinstance(statistics["gameCount"], int) and statistics["gameCount"] > 0, result
    assert sum(statistics["scoreDistribution"].values()) == statistics["gameCount"], result
    print("PASS: Backend -> Floci -> Simulator -> Floci -> Backend")
    print(json.dumps(statistics, ensure_ascii=False))


if __name__ == "__main__":
    main()
