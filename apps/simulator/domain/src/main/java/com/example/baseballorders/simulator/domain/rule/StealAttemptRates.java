package com.example.baseballorders.simulator.domain.rule;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * 盗塁戦略が盗塁を企図する割合の設定。
 *
 * @param toDoubleAttemptRate 一塁走者が二塁を狙う割合
 * @param toTripleAttemptRate 二塁走者が三塁を狙う割合
 */
@Builder(style = BuilderStyle.STAGED)
public record StealAttemptRates(float toDoubleAttemptRate, float toTripleAttemptRate) {}
