# 変更概要

status: done

- simulationのリクエストに、選手の性格を追加。
- 性格は、Default,EagerSluggish,EagerSteal,EagerBuntの４択。
- 性格は既存のBehaviorクラスに紐付ける。

# ここから先は、上の機能の実装のレビューが完了してから着手する
- domain/model/behavior配下のクラスをsealedにして、戦略の見通しをよくする
