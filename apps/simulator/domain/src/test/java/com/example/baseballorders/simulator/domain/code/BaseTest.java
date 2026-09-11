package com.example.baseballorders.simulator.domain.code;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseTest {

    @Test
    @DisplayName("各塁は本塁からの数値を返す")
    void returnsNumberOfEachBase() {
        // given
        Base first = Base.FIRST;
        Base second = Base.SECOND;
        Base third = Base.THIRD;

        // when
        int firstNumber = first.getNumber();
        int secondNumber = second.getNumber();
        int thirdNumber = third.getNumber();

        // then
        assertAll(
                () -> assertEquals(1, firstNumber),
                () -> assertEquals(2, secondNumber),
                () -> assertEquals(3, thirdNumber));
    }
}
