package com.example.baseballorders.simulator.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * 設定ファイルの確率値がプロパティへ束縛され、ドメインの確率設定へ変換されることを確認する。
 *
 * <p>実物: {@code application.yml} と各プロファイルの実ファイル、Spring Bootの {@code Binder}、{@link
 * SimulationRuleProperties}、Jilt生成のStaged Builder。
 *
 * <p>モック: なし。
 *
 * <p>担保する疎通: yml -> YamlPropertySourceLoader -> Binder -> SimulationRuleProperties ->
 * SimulationRules。
 *
 * <p>担保しないもの: Spring Bootの起動時に実際にこのプロパティが注入されるか（{@code SimulatorApplicationTest}
 * が担保する）。乱数を使った試合の結果。
 */
class SimulationRulePropertiesTest {

    @DisplayName("全プロファイルの設定ファイルが同じ確率値をドメインの確率設定へ渡す")
    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "application.yml",
                "application-local.yml",
                "application-dev.yml",
                "application-prod.yml"
            })
    void bindsProbabilitiesFromEveryProfile(String resourceName) throws IOException {
        // given
        var binder = binderFor(resourceName);

        // when
        var rules =
                binder.bind("simulation.rule", SimulationRuleProperties.class)
                        .get()
                        .toSimulationRules();

        // then
        assertAll(
                resourceName,
                () -> assertEquals(0.05f, rules.batting().walkProbability()),
                () -> assertEquals(0.25f, rules.batting().strikeoutProbabilityWhenNotOnBase()),
                () -> assertEquals(6f, rules.middleDistanceHitting().doubleDivisor()),
                () -> assertEquals(6f, rules.middleDistanceHitting().tripleDivisor()),
                () -> assertEquals(6f, rules.middleDistanceHitting().homeRunDivisor()),
                () -> assertEquals(2f, rules.middleDistanceHitting().singleReductionDivisor()),
                () -> assertEquals(8f, rules.longDistanceHitting().doubleDivisor()),
                () -> assertEquals(8f, rules.longDistanceHitting().tripleDivisor()),
                () -> assertEquals(2f, rules.longDistanceHitting().homeRunDivisor()),
                () -> assertEquals(1f, rules.longDistanceHitting().singleReductionDivisor()),
                () -> assertEquals(0.2f, rules.standardSteal().toDoubleAttemptRate()),
                () -> assertEquals(0.05f, rules.standardSteal().toTripleAttemptRate()),
                () -> assertEquals(0.3f, rules.eagerSteal().toDoubleAttemptRate()),
                () -> assertEquals(0.15f, rules.eagerSteal().toTripleAttemptRate()),
                () -> assertEquals(0.2f, rules.runnerAdvance().fromFirstProbability()),
                () -> assertEquals(0.2f, rules.runnerAdvance().fromSecondProbability()),
                () -> assertEquals(0.1f, rules.runnerAdvance().fromThirdProbability()));
    }

    @DisplayName("全プロファイルの設定ファイルが同じ投手補正倍率を渡す")
    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "application.yml",
                "application-local.yml",
                "application-dev.yml",
                "application-prod.yml"
            })
    void bindsPitcherMultipliersFromEveryProfile(String resourceName) throws IOException {
        // given
        var binder = binderFor(resourceName);

        // when
        var pitcher = binder.bind("simulation.pitcher", SimulationPitcherProperties.class).get();

        // then
        assertAll(
                resourceName,
                () -> assertEquals(1.3f, pitcher.bold().onBaseMultiplier()),
                () -> assertEquals(0.7f, pitcher.bold().sluggingMultiplier()),
                () -> assertEquals(1.0f, pitcher.bold().runningMultiplier()),
                () -> assertEquals(0.7f, pitcher.cautious().onBaseMultiplier()),
                () -> assertEquals(1.0f, pitcher.cautious().sluggingMultiplier()),
                () -> assertEquals(1.3f, pitcher.cautious().runningMultiplier()),
                () -> assertEquals(1.0f, pitcher.technical().onBaseMultiplier()),
                () -> assertEquals(1.3f, pitcher.technical().sluggingMultiplier()),
                () -> assertEquals(0.7f, pitcher.technical().runningMultiplier()),
                () -> assertEquals(1.0f, pitcher.standard().onBaseMultiplier()),
                () -> assertEquals(1.0f, pitcher.standard().sluggingMultiplier()),
                () -> assertEquals(1.0f, pitcher.standard().runningMultiplier()));
    }

    private static Binder binderFor(String resourceName) throws IOException {
        List<PropertySource<?>> loaded =
                new YamlPropertySourceLoader()
                        .load(resourceName, new ClassPathResource(resourceName));
        var propertySources = new MutablePropertySources();
        loaded.forEach(propertySources::addLast);
        return new Binder(
                ConfigurationPropertySources.from(propertySources),
                new PropertySourcesPlaceholdersResolver(propertySources));
    }
}
