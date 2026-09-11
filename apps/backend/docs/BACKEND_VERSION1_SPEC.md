# baseball-orders backend — Version 1 Specification

## 1. Purpose

`backend` is the user-facing application of `baseball-orders`.

Version 1 focuses on validating synchronous HTTP request handling combined with asynchronous SQS-based processing.

The backend must:

- provide the web UI with Spring MVC and Thymeleaf;
- receive a simulation request from the browser;
- read the required source data from an H2 in-memory database;
- generate a unique `simulationId`;
- send a simulation request to SQS;
- wait for the corresponding simulation result;
- receive simulation results from SQS using `@SqsListener`;
- correlate the received result with the waiting HTTP request;
- return the simulation result as a Thymeleaf response.

Future customer-management and order-management functionality may be added to this application, but is outside Version 1 scope.

---

## 2. External Dependencies

The backend depends on:

- Spring Boot
- Spring MVC
- Thymeleaf
- H2
- Amazon SQS compatible messaging
- Spring Cloud AWS SQS support
- the shared `messaging-contract` module

The backend must not depend directly on the `simulator` application.

---

## 3. Runtime Assumptions

Version 1 assumes:

- exactly one backend application instance;
- H2 is an in-memory database;
- the backend process may lose all in-memory state when restarted;
- simulation processing completes within 30 seconds under normal conditions;
- SQS is used for communication with `simulator`.

Horizontal scaling of the backend is explicitly outside Version 1 scope.

---

## 4. User Flow

Normal flow:

```text
Browser
  |
  | POST simulation request
  v
backend
  |
  | read source data from H2
  | generate simulationId
  | register waiting result
  | send request message
  v
simulation-request SQS
  |
  v
simulator
  |
  v
simulation-result SQS
  |
  v
backend @SqsListener
  |
  | complete waiting result
  v
HTTP request resumes
  |
  v
Thymeleaf result page
  |
  v
Browser
```

Version 1 does not return `202 Accepted`.

The original HTTP request remains open until the simulation result is received or a timeout occurs.

---

## 5. Web UI

The backend must provide:

- a page for entering or selecting simulation parameters;
- submission of a simulation request;
- a result page;
- an error page for timeout or processing failures.

Polling, SSE, WebSocket, and SPA frameworks are not used in Version 1.

---

## 6. H2

Version 1 does not use an external database.

H2 is used only inside the backend application.

Example JDBC mode:

```text
jdbc:h2:mem:baseballorders
```

H2 may contain:

- source data needed for simulation;
- prototype customer data;
- prototype order data.

Simulation results do not need to be persisted in Version 1.

Loss of H2 data after application restart is acceptable.

---

## 7. Correlation

Every simulation request must have a unique:

```text
simulationId
```

Type:

```text
UUID
```

The same `simulationId` must be included in:

- the request message sent to SQS;
- the result message returned by `simulator`;
- logs related to the simulation;
- the backend's in-memory waiting-result registry.

The `simulationId` acts as the correlation ID for the request/reply flow.

---

## 8. Waiting for the Result

The backend must maintain an in-memory association equivalent to:

```text
simulationId -> CompletableFuture<SimulationResult>
```

A thread-safe structure must be used because:

- the HTTP request is handled by one thread;
- the SQS listener may receive the result on another thread;
- multiple HTTP requests may be processed concurrently.

The implementation must support:

- registering a waiting request;
- completing the corresponding wait when a result arrives;
- removing the wait after success;
- removing the wait after timeout;
- removing the wait after send failure.

No pending wait entry may remain indefinitely.

---

## 9. Request Message

The backend sends messages to:

```text
simulation-request
```

The request message must include at least:

- `simulationId`;
- message/schema version;
- simulation input.

The backend must convert its own domain objects to shared message-contract objects.

Backend domain objects must not be exposed directly through the messaging contract.

---

## 10. Result Message

The backend listens to:

```text
simulation-result
```

using:

```java
@SqsListener("simulation-result")
```

When a result is received, the backend must:

1. read `simulationId`;
2. convert the message payload to the backend representation;
3. locate the waiting request associated with that `simulationId`;
4. complete it with the received result.

If no waiting request exists, the message may be ignored in Version 1 and a warning should be logged.

Possible reasons include:

- the HTTP request already timed out;
- the backend was restarted;
- a duplicate result arrived.

---

## 11. Timeout

The backend must never wait indefinitely for a result.

Version 1 timeout:

```text
30 seconds
```

If no result arrives within 30 seconds:

- stop waiting;
- remove the pending in-memory entry;
- return an error response/page;
- log the timeout with `simulationId`.

The user-facing error should indicate that the result could not be obtained within the allowed time.

A timeout does not necessarily mean that the simulator itself failed.

---

## 12. Send Failure

If sending the request message to SQS fails:

- the backend must not continue waiting for a result;
- the pending wait entry must be removed;
- the HTTP request must fail appropriately;
- the error must be logged with `simulationId`.

---

## 13. Duplicate Result Handling

SQS Standard Queue may deliver messages more than once.

Version 1 must tolerate duplicate result messages.

A duplicate result must not corrupt another request or produce a second HTTP response.

If the corresponding wait is already completed or removed, the duplicate result may be ignored.

---

## 14. Logging

At minimum, log the following lifecycle events:

```text
simulation request accepted
simulation request sent
simulation result received
simulation completed
simulation timeout
simulation request send failed
```

All simulation-related logs should include `simulationId`.

MDC may be used.

---

## 15. Testing

### Unit tests

Docker is not required.

Test at minimum:

- waiting-result registration and completion;
- timeout cleanup;
- cleanup after send failure;
- request/reply correlation;
- duplicate result behavior;
- conversion between backend data and messaging-contract data.

### Messaging integration tests

LocalStack + Testcontainers may be used for SQS integration tests.

RabbitMQ must not be used as an SQS substitute.

The backend application itself does not need to be Dockerized for Version 1 tests.

---

## 16. Constraints

Version 1 intentionally accepts these limitations:

- one backend instance only;
- in-memory correlation state;
- no persistent job state;
- HTTP connection remains open during simulation;
- backend restart loses all pending requests;
- timeout results received later are discarded;
- H2 data is non-persistent.

These are known trade-offs rather than defects for Version 1.

---

## 17. Non-Goals

Do not implement the following in Version 1:

- backend horizontal scaling;
- Polling;
- SSE;
- WebSocket;
- persistent simulation-job storage;
- PostgreSQL/RDS/Aurora;
- DynamoDB;
- Redis;
- Transactional Outbox;
- Terraform;
- ECS/EKS/Kubernetes;
- production authentication/authorization;
- exactly-once processing.

---

## 18. Acceptance Criteria

Version 1 backend is complete when:

1. the backend starts as a Spring Boot application;
2. the simulation input page can be displayed;
3. source data can be read from H2;
4. a unique `simulationId` is generated per request;
5. a request message can be sent to `simulation-request`;
6. the HTTP request waits for the result;
7. `simulation-result` is consumed using `@SqsListener`;
8. the received result completes the correct waiting HTTP request;
9. the result is rendered with Thymeleaf;
10. a missing result times out after 30 seconds;
11. pending waits are cleaned up after success, timeout, and send failure;
12. duplicate or late result messages do not corrupt another request;
13. the backend has no direct application dependency on `simulator`.
