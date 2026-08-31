# Development Instructions

## Autonomous execution

- Do not pause to ask the user questions or request confirmation.
- When a requirement is ambiguous, choose the safest reasonable assumption and continue.
- If an operation is unavailable, try a safe alternative that remains within the requested scope.
- Continue through implementation, build, tests, and reasonable fixes until the task is complete.
- Report assumptions, unresolved blockers, and verification results in the final response.

## Gradle dependency protection

- Do not add, remove, upgrade, downgrade, or otherwise change dependency declarations in `build.gradle` or `build.gradle.kts` files.
- Do not change dependency versions indirectly through version catalogs, dependency constraints, plugin declarations, `settings.gradle`, or `settings.gradle.kts`.
- Existing dependencies may be inspected and used as-is.
- If the requested work appears to require a dependency change, implement the best solution possible with the existing dependencies and report the limitation in the final response.


## Test-Driven Development

- Implement all changes using test-driven development (TDD).
- Before changing production code, write or update a test that describes the expected behavior and confirm that it fails for the expected reason.
- Implement only the minimum production code required to make the test pass.
- Refactor only after the tests pass, and keep the test suite passing throughout refactoring.
- Run the relevant tests after each change and run the full test suite before completing the task.

## Java Test Style

- Apply the following rules to every Java test method, including newly added and modified parameterized tests.
- Add `@DisplayName` to every test method. Its value must be a clear Japanese sentence describing the expected behavior. Do not use an English display name or merely repeat the method name.
- Structure each test with the comments `// given`, `// when`, and `// then`, in that order. Put setup, execution, and verification in their corresponding sections.
- Wrap assertions in `assertAll`, including when a test currently contains only one assertion. Use `org.junit.jupiter.api.Assertions.assertAll`.
- When verifying an exception, perform the action with `assertThrows` in the `// when` section and verify the captured exception inside `assertAll` in the `// then` section.
- Keep each test focused on one behavior. Multiple assertions are allowed only when they jointly verify that behavior.

Example:

```java
@Test
@DisplayName("有効な打順を指定すると選手が登録される")
void registersPlayerWhenBattingOrderIsValid() {
    // given
    var order = new BattingOrder();
    var player = new Player("山田");

    // when
    order.register(1, player);

    // then
    assertAll(
            () -> assertEquals(player, order.playerAt(1)),
            () -> assertEquals(1, order.size())
    );
}
```

## Test Coverage

- Achieve 100% C2 condition coverage for all production code added or changed by the task.
- For every atomic boolean condition, add tests that independently exercise both `true` and `false`, including conditions combined with `&&` or `||`, short-circuit behavior, ternary expressions, loop conditions, and each `case` or default path.
- Do not treat line coverage alone as sufficient. If the configured coverage tool reports branch coverage rather than C2 condition coverage directly, inspect every compound condition and add the test cases required to prove that each atomic condition evaluates to both outcomes.
- Do not exclude, suppress, or weaken coverage checks merely to reach 100%.
- Before completing the task, run the configured coverage report and confirm the C2 result. If the project has no coverage command or cannot measure C2 accurately, report that limitation explicitly; do not claim 100% based only on line or branch coverage.

## Javadoc

- Add Javadoc to every public method in production Java code, except overridden methods.
- Do not add Javadoc to public methods annotated with `@Override`.
- Describe the method's purpose and observable behavior.
- Document every parameter with `@param`, non-void return values with `@return`, and declared exceptions with `@throws`.
- Update the Javadoc whenever a public method's behavior or signature changes.