package com.example.baseballorders.simulator.domain.statistics;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * 1試合実行で発生した1つのプレー結果を表す状況推移。
 *
 * <p>{@code outCount} と {@code cumulativeScore} と {@code runnerState} は、{@link
 * PlayResultObserver}の契約どおり、この推移自身の塁状態遷移が適用される直前（＝それまでの全プレー適用後）の状況を表す。
 *
 * @param inning この推移が発生したイニング
 * @param actionResult 打者または走者の振る舞い結果を表す日本語の説明
 * @param outCount この推移が発生した時点のアウト数
 * @param cumulativeScore この推移が発生した時点までの累積得点
 * @param runnerState この推移が発生した時点の走者状況を表す日本語の説明
 */
@Builder(style = BuilderStyle.STAGED)
public record GameTransition(
        long inning, String actionResult, int outCount, long cumulativeScore, String runnerState) {}
