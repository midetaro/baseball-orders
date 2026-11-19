package com.example.baseballorders.backend.infrastructure.web;

import java.util.List;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * 打順組み替え画面のチーム選択肢と、そのチームの既定打者の表示用モデル。
 *
 * @param key チームを識別するキー（列挙名）
 * @param label 画面に表示するチーム名
 * @param batters 打順順の既定打者
 */
@Builder(style = BuilderStyle.STAGED)
public record TeamLineupView(String key, String label, List<DefaultBatterView> batters) {}
