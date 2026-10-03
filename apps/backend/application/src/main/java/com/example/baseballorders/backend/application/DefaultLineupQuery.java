package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.application.adapter.DefaultLineupRepository;
import com.example.baseballorders.backend.domain.TeamDefaultLineup;
import java.util.List;
import java.util.Objects;

/** 打順組み替え画面に表示するチーム別の既定オーダーを取得するユースケース。 */
public final class DefaultLineupQuery {

    private final DefaultLineupRepository repository;

    /**
     * リポジトリを指定してユースケースを生成する。
     *
     * @param repository 既定オーダーの取得ポート
     */
    public DefaultLineupQuery(DefaultLineupRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * 選択可能な全チームの既定オーダーを取得する。
     *
     * @return 表示順に並んだ不変の既定オーダー一覧
     */
    public List<TeamDefaultLineup> findAllTeams() {
        return List.copyOf(repository.findAll());
    }
}
