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
- Write or update a failing test that describes the expected behavior before changing production code.
- Implement the minimum production code required to make the test pass.
- Refactor only after the tests pass, and keep the test suite passing throughout the refactoring.
- Run the relevant tests after each change and run the full test suite before completing the task.

## Javadoc

- Add Javadoc to every public method in production Java code, except overridden methods.
- Do not add Javadoc to public methods annotated with `@Override`.
- Describe the method's purpose and observable behavior.
- Document every parameter with `@param`, non-void return values with `@return`, and declared exceptions with `@throws`.
- Update the Javadoc whenever a public method's behavior or signature changes.