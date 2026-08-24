## フォルダ構成

```
baseball-orders/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
│
├── compose.yaml
├── README.md
│
├── apps/
│   ├── backend/
│   │   ├── build.gradle
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/
│   │       │   │   └── com/example/baseballorders/backend/
│   │       │   │       ├── BackendApplication.java
│   │       │   │       │
│   │       │   │       ├── simulation/
│   │       │   │       │   ├── presentation/
│   │       │   │       │   ├── application/
│   │       │   │       │   ├── domain/
│   │       │   │       │   └── infrastructure/
│   │       │   │       │       ├── persistence/
│   │       │   │       │       └── messaging/
│   │       │   │       │
│   │       │   │       └── common/
│   │       │   │           ├── exception/
│   │       │   │           └── config/
│   │       │   │
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       ├── application-local.yml
│   │       │       └── schema.sql
│   │       │
│   │       └── test/
│   │           └── java/
│   │               └── com/example/baseballorders/backend/
│   │                   └── simulation/
│   │
│   └── simulator/
│       ├── build.gradle
│       └── src/
│           ├── main/
│           │   ├── java/
│           │   │   └── com/example/baseballorders/simulator/
│           │   │       ├── SimulatorApplication.java
│           │   │       │
│           │   │       ├── application/
│           │   │       │   └── ExecuteSimulationUseCase.java
│           │   │       │
│           │   │       ├── domain/
│           │   │       │
│           │   │       └── infrastructure/
│           │   │           └── messaging/
│           │   │
│           │   └── resources/
│           │       ├── application.yml
│           │       └── application-local.yml
│           │
│           └── test/
│               └── java/
│                   └── com/example/baseballorders/simulator/
│                       ├── application/
│                       └── domain/
│
├── libs/
│   └── messaging-contract/
│       ├── build.gradle
│       └── src/
│           ├── main/
│           │   └── java/
│           │       └── com/example/baseballorders/messaging/
│           └── test/
│
├── docker/
│   ├── backend.Dockerfile
│   └── simulator.Dockerfile
│
└── docs/
    ├── architecture.md
    ├── sequence-diagram.md
    ├── message-contract.md
    └── failure-scenarios.md
```