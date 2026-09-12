package com.example.baseballorders.backend.application.adapter;

import com.example.baseballorders.backend.application.dto.PlayerListItem;
import java.util.List;

/** 画面に表示する選手一覧を取得するポート。 */
@FunctionalInterface
public interface PlayerListQuery {

    /**
     * 表示可能な選手一覧を取得する。
     *
     * @return 選手一覧
     */
    List<PlayerListItem> findAll();
}
