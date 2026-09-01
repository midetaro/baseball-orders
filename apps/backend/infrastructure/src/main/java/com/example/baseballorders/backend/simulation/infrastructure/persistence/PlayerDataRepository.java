package com.example.baseballorders.backend.simulation.infrastructure.persistence;

import com.example.baseballorders.backend.simulation.infrastructure.messaging.PlayerData;
import java.util.List;

/** player IDに対応する選手の打撃データを取得するRepository。 */
@FunctionalInterface
public interface PlayerDataRepository {

    /**
     * 指定されたplayer IDの順序を維持して選手データを取得する。
     *
     * @param playerIds 取得するplayer ID
     * @return IDの入力順に並んだ選手データ
     * @throws IllegalArgumentException 存在しないplayer IDが含まれる場合
     */
    List<PlayerData> findAllByIds(List<Long> playerIds);
}
