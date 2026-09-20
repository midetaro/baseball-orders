package com.example.baseballorders.simulator.domain.model.situation.base;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.example.baseballorders.simulator.domain.entity.behavior.batting.LongDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.batting.MiddleDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.batting.ShortDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.bunt.StandardBuntStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.steal.EagerStealStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.steal.NowayStealStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.steal.StandardStealStrategy;
import com.example.baseballorders.simulator.domain.model.base.BaseStateFactory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseStateComponentTest {

    @DisplayName("domainの状態ファクトリと戦略はSpringアノテーションに依存しない")
    @Test
    void domainTypesDoNotDependOnSpringAnnotations() {
        // given
        List<Class<?>> domainTypes =
                List.of(
                        BaseStateFactory.class,
                        LongDistanceHittingStrategy.class,
                        MiddleDistanceHittingStrategy.class,
                        ShortDistanceHittingStrategy.class,
                        StandardBuntStrategy.class,
                        EagerStealStrategy.class,
                        NowayStealStrategy.class,
                        StandardStealStrategy.class);

        // when
        var annotationTypeNames =
                domainTypes.stream()
                        .flatMap(type -> java.util.Arrays.stream(type.getAnnotations()))
                        .map(annotation -> annotation.annotationType().getName())
                        .toList();

        // then
        assertAll(
                () ->
                        assertFalse(
                                annotationTypeNames.stream()
                                        .anyMatch(name -> name.startsWith("org.springframework"))));
    }
}
