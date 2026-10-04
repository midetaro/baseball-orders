package com.example.baseballorders.backend.infrastructure.web;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * 打順組み替え画面に表示する既定打者の表示用モデル。
 *
 * @param battingOrder 打順（1〜9）
 * @param hitAverage 小数第3位まで整形した打率
 * @param personality 性格の列挙名
 * @param stealForced 盗塁が固定で有効な打者かどうか
 * @param buntForced バントが固定で有効な打者かどうか
 */
@Builder(style = BuilderStyle.STAGED)
public record DefaultBatterView(
        int battingOrder,
        String hitAverage,
        String personality,
        boolean stealForced,
        boolean buntForced) {}
