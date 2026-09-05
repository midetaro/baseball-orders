package com.example.baseballorders.simulator.domain.code;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OutCountTest {

    @DisplayName("アウトを加算すると三アウトを上限としたアウトカウントを返す")
    @ParameterizedTest(name = "{0}")
    @MethodSource("advanceTestCases")
    void advancesOutCount(String description, OutCount current, long increment, OutCount expected) {
        // given

        // when
        OutCount result = current.add(increment);

        // then
        assertAll(() -> assertEquals(expected, result, description));
    }

    static Stream<Arguments> advanceTestCases() {
        return Stream.of(
                arguments("無死に一アウトを加えると一死になる", OutCount.NO_OUT, 1, OutCount.ONE_OUT),
                arguments("無死に二アウトを加えると二死になる", OutCount.NO_OUT, 2, OutCount.TWO_OUT),
                arguments("一死に二アウトを加えると三アウトになる", OutCount.ONE_OUT, 2, OutCount.THREE_OUT),
                arguments("二死に二アウトを加えても三アウトにとどまる", OutCount.TWO_OUT, 2, OutCount.THREE_OUT));
    }
}
