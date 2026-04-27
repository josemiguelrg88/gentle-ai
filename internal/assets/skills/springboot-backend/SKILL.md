---
name: springboot-backend
description: >
  Spring Boot/Java backend conventions for SDD work: OpenAPI per domain, layered tests, DTO boundaries, manual mappers, Testcontainers, Surefire/Failsafe, and JaCoCo.
  Trigger: When working on Java/Spring Boot backend services, Maven/Gradle projects with @SpringBootApplication, @RestController, pom.xml/build.gradle Spring Boot plugins, or src/main/java Spring services.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
---

## When to Use

Use this skill when the project or task involves:
- Java/Spring Boot backend services.
- `pom.xml`, `build.gradle`, or `build.gradle.kts` with Spring Boot dependencies/plugins.
- `@SpringBootApplication`, `@RestController`, `@Service`, `@Repository`, JPA entities, or Spring MVC APIs.
- SDD phases that create/change backend endpoints, controllers, services, repositories, DTOs, OpenAPI contracts, tests, or integration infrastructure.

## Critical Patterns

### 1. Detect versions before implementation

Before coding, detect and record:
- JDK version from Maven/Gradle toolchains, compiler config, `sourceCompatibility`, `targetCompatibility`, `.java-version`, `.sdkmanrc`, Dockerfile, or CI.
- Spring Boot version from `spring-boot-starter-parent`, `spring-boot-dependencies` BOM, Maven/Gradle plugin `org.springframework.boot`, or version properties.

If either version cannot be detected, ask the user before implementation and record the answer in `design.md`/`tasks.md`.

### 2. Initial scope: single-module Spring Boot

The first iteration supports single-module Spring Boot projects.
If a multi-module project is detected, warn the user and ask which module is the main Spring Boot module before continuing in limited mode.

### 3. OpenAPI per domain/controller

- Create or update one OpenAPI contract per service domain/controller.
- Store contracts at `src/main/resources/docs/openapi/{domain}.yml`.
- Use kebab-case for `{domain}`.
- Example: `CustomerManagementController` → `src/main/resources/docs/openapi/customer-management.yml`.
- If multiple controllers look like one functional domain, ask the user before grouping them into one OpenAPI.

### 4. Package by domain

Organize code by domain, with each domain owning its layers:

```text
com.example.<app>.<domain>.controller
com.example.<app>.<domain>.dto.request
com.example.<app>.<domain>.dto.response
com.example.<app>.<domain>.mapper
com.example.<app>.<domain>.service
com.example.<app>.<domain>.repository
com.example.<app>.<domain>.entity
```

### 5. DTO boundaries and manual mappers

- Request DTOs and response DTOs MUST be independent.
- The request/response API layer MUST be independent from internal domain/application/persistence models.
- Do NOT expose JPA entities or internal models directly from controllers or OpenAPI contracts.
- Services orchestrate mapping between request DTOs, internal models/entities, and response DTOs.
- Prefer explicit/manual mapper classes or methods. Do NOT use MapStruct.
- Mappers must be simple, testable, and free of business logic.

### 6. Testing by layer and use case/feature

Group tests with `@Nested` classes by use case or feature. Each use case must be self-contained and must not depend on execution order or shared mutable fixtures. Add `@DisplayName` descriptions based on the current ticket, development branch, or SDD definition chosen by the user.

| Layer | Preferred test style |
|-------|----------------------|
| Domain/service logic | Unit tests with Mockito for external collaborators |
| Controllers | MVC tests, preferably `@WebMvcTest` |
| Repositories | `@DataJpaTest` when JPA queries/mappings matter |
| Services needing real JPA | `@DataJpaTest` only when persistence behavior is the point |
| Full integration | `@SpringBootTest` under `src/test-integration` |
| Non-trivial mappers | Unit tests for manual mapper behavior |

Avoid `@SpringBootTest` for unit or slice tests.

### 7. Test data isolation

- Test cases must be autocontained: clear arrange/act/assert and local fixtures/builders.
- Test data must not interfere between executions or between `@Nested` use cases.
- Prefer per-test/per-`@Nested` builders or factories over mutable global fixtures.
- When persistence is involved, isolate state with rollback, controlled truncation, isolated schemas, or ephemeral containers as appropriate.

### 8. Integration tests

- Integration tests live in `src/test-integration/java`.
- Integration resources live in `src/test-integration/resources`.
- Integration config uses `app-profile.yml` in the integration resources folder.
- Integration tests use `@SpringBootTest`.
- Use Testcontainers when external dependencies are needed: databases, queues, brokers, caches, or auxiliary services.
- For each Testcontainer, propose and define min and max memory based on the service/container and use case requirements.
- Justify why each container is necessary and what it validates.

### 9. Maven/Gradle test plugins

For Maven:
- Surefire runs unit and slice tests.
- Failsafe runs integration tests.
- JaCoCo covers both unit/slice and integration tests.
- Configure integration source roots for `src/test-integration/java` and `src/test-integration/resources`.
- Bind Failsafe to `integration-test`/`verify`.

For Gradle:
- Use equivalent `testIntegration` or `integrationTest` source set/task.
- Keep paths `src/test-integration/java` and `src/test-integration/resources`.
- Aggregate JaCoCo coverage from unit/slice and integration tasks when supported.

Coverage thresholds:
- Respect existing JaCoCo thresholds.
- If none exist, propose an initial threshold (for example 80% lines / 70% branches) and let the user choose or confirm.

## SDD Phase Checklist

### sdd-explore
- Detect Spring Boot/JDK versions, build tool, module layout, controllers, domains, persistence, and external dependencies.
- If JDK/Spring Boot versions are missing, stop and ask the user before implementation planning.

### sdd-propose
- Identify affected domains/controllers.
- State which OpenAPI file(s) under `src/main/resources/docs/openapi/{domain}.yml` will be created or changed.
- State whether Testcontainers are expected and why.

### sdd-spec
- Define use cases/features as self-contained scenarios and choose the `@DisplayName` traceability label from ticket, branch, or user-provided definition.
- Include API contract expectations per domain OpenAPI.
- Define test data isolation requirements.

### sdd-design
- Document package-by-domain layout.
- Document request/response DTO boundaries and manual mapper responsibilities.
- Document testing strategy by layer, integration source set, `app-profile.yml`, Testcontainers, Surefire/Failsafe, and JaCoCo.

### sdd-tasks
- Split work by domain/layer: OpenAPI, DTOs, mappers, controller, service, repository/entity, tests, build config.
- Include tasks for unit/slice/integration tests, `@DisplayName` labels, and coverage config.

### sdd-apply
- Follow TDD when strict mode is enabled.
- Write layer-appropriate tests first.
- Keep use cases isolated and avoid global mutable fixtures.

### sdd-verify
- Verify OpenAPI per domain/controller.
- Verify no internal entities/models leak into API DTOs or contracts.
- Verify no MapStruct usage was introduced.
- Run Surefire, Failsafe, and JaCoCo commands appropriate to the build tool.
- Verify Testcontainers have justified min/max memory when used.
- Verify `@DisplayName` labels are descriptive and traceable to the ticket/branch/SDD definition selected by the user.

## Commands

```bash
# Maven unit/slice tests
./mvnw test

# Maven integration tests + verification
./mvnw verify

# Maven compile fast check
./mvnw -q -DskipTests compile

# Gradle unit tests
./gradlew test

# Gradle integration tests (if configured)
./gradlew integrationTest

# Gradle full verification
./gradlew check
```

## Resources

- `assets/unit-service-test.java` — Mockito + `@Nested` + `@DisplayName` unit/service test structure.
- `assets/webmvc-controller-test.java` — `@WebMvcTest` controller test structure with request/response assertions.
- `assets/integration-test.java` — `@SpringBootTest` + Testcontainers integration test structure under `src/test-integration`.
