package com.example.baseballorders.simulator.domain.rule;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * 長打率と出塁率の差（長打によって増えた塁数）を安打種別へ配分する除数設定。
 *
 * <p>各除数は「差をいくつで割った値をその安打種別の重みにするか」を表す。除数が小さいほどその種別へ多く配分される。
 *
 * @param doubleDivisor 二塁打の重みを求める除数
 * @param tripleDivisor 三塁打の重みを求める除数
 * @param homeRunDivisor 本塁打の重みを求める除数
 * @param singleReductionDivisor 単打の重みから差し引く量を求める除数
 */
@Builder(style = BuilderStyle.STAGED)
public record HittingDistribution(
        float doubleDivisor,
        float tripleDivisor,
        float homeRunDivisor,
        float singleReductionDivisor) {}
