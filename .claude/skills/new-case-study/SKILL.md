---
name: new-case-study
description: Scaffold a new backend engineering case study in this repo as a fully standalone Spring Boot project in its own folder, structured with strict hexagonal architecture (ports and adapters) and DDD, with its own Maven build and its own docker-compose running PostgreSQL. Use this whenever the user asks to create, start, add, or scaffold a new case study, use case, problem, or project in this repo (e.g. "new case study about idempotent payments", "اعمل case study جديدة عن rate limiting", "ابدأ بروجكت جديد"), even if they don't mention the skill by name.
---

# New case study

This repo is a collection of backend engineering case studies. Each one lives in
its own top-level folder and is a completely independent project: its own
`pom.xml`, its own Maven wrapper, its own `docker-compose.yml`. Someone should be
able to `cd` into one folder and run it without knowing the others exist.

That independence is the point of the repo layout, so:

- Never create a root/parent `pom.xml` and never add `<parent>` or `<modules>`
  links between case studies.
- Never share a docker-compose file, network, or volume between case studies.
- Never modify another case study's folder while scaffolding a new one.

## Inputs

You need one thing from the user: what problem the case study is about. A short
phrase is enough ("idempotent payment API", "outbox pattern"). If they gave it,
don't ask anything else; derive the rest. If they gave nothing, ask for the
problem in one question.

## Step 1: Work out the name, number, and ports

1. List the top-level folders. Case studies are named `NN-kebab-slug`
   (`01-idempotent-payments`, `02-outbox-pattern`). The new one gets the next
   number and a short slug (2-4 words) derived from the problem.
2. Ports come from the number `NN`, so several case studies can run at once:
  - PostgreSQL host port: `54NN` (case study 03 → `5403`)
  - Application port: `81NN` (case study 03 → `8103`)
3. Maven coordinates:
  - `groupId`: reuse the groupId from an existing case study's `pom.xml`. If
    this is the first one, use `com.kidwany.casestudies`.
  - `artifactId`: the slug without the number (`idempotent-payments`).
  - Base package: groupId + slug with hyphens removed
    (`com.kidwany.casestudies.idempotentpayments`).

## Step 2: Resolve versions

Java is fixed at 21. Spring Boot and PostgreSQL versions go stale, so look them
up at scaffold time instead of trusting memory:

```bash
curl -s -H 'Accept: application/json' https://start.spring.io/metadata/client
```

From the response take:

- **Java**: always **21** (LTS). This is the repo's fixed choice; don't
  substitute a newer version even if one is available.
- **Spring Boot**: the default `bootVersion` (the current stable release; never a
  SNAPSHOT, milestone, or RC).

For PostgreSQL, use the current stable major version, pinned to the major
(`postgres:18-alpine`, not `latest`), so the case study stays reproducible.

## Step 3: Generate the project with Spring Initializr

Generating from start.spring.io guarantees the Boot version, starters, and Maven
wrapper are compatible with each other, which hand-written poms often get wrong
across major versions.

```bash
curl -s https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d javaVersion=21 \
  -d groupId=<GROUP_ID> \
  -d artifactId=<ARTIFACT_ID> \
  -d name=<ARTIFACT_ID> \
  -d packageName=<BASE_PACKAGE> \
  -d dependencies=web,data-jpa,postgresql,flyway,validation,actuator,lombok,testcontainers \
  -o /tmp/case-study.zip
mkdir <NN-slug> && unzip -q /tmp/case-study.zip -d <NN-slug> && rm /tmp/case-study.zip
```

Baseline dependencies and why they are there:

| Dependency | Purpose |
|---|---|
| `web` | REST endpoints to drive the scenario |
| `data-jpa` + `postgresql` | Persistence |
| `flyway` | Schema as versioned SQL, so the DB state is explicit |
| `validation` | Request validation |
| `actuator` | Health endpoint and basic metrics |
| `lombok` | Less boilerplate |
| `testcontainers` | Integration tests against real Postgres |

If the problem clearly needs something more (e.g. Redis for a rate limiter,
Kafka for an outbox relay), tell the user what you'd add and why, and add it
only after they agree. The default stays Postgres only.

If start.spring.io is unreachable, write the `pom.xml` by hand with
`spring-boot-starter-parent` as its parent (that is the Spring Boot BOM, not a
repo parent, so it's fine) and the same dependencies, and tell the user the
versions were not verified online.

## Step 4: Add Dockerfile and docker-compose.yml

The case study must be runnable by someone with only Docker installed (no JDK,
no Maven, no IDE): `docker compose up --build` builds the app inside a container
and starts it next to Postgres. Java developers can still run just Postgres in
Docker and the app from their IDE.

Create `<NN-slug>/Dockerfile` (multi-stage: the JDK image builds the jar, the
smaller JRE image runs it):

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 81NN
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- Dependencies are resolved in their own layer before `src/` is copied, so
  rebuilds after a code change don't re-download everything.
- Tests are skipped in the image build because the integration tests use
  Testcontainers, which needs a Docker daemon that isn't available inside
  `docker build`. Tests run with `./mvnw verify` on the host.

Create `<NN-slug>/.dockerignore`:

```
target/
.git/
.idea/
*.iml
.vscode/
```

Create `<NN-slug>/docker-compose.yml`:

```yaml
name: <NN-slug>

services:
  postgres:
    image: postgres:<PG_MAJOR>-alpine
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    ports:
      - "54NN:5432"
    volumes:
      - pgdata:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d app"]
      interval: 5s
      timeout: 3s
      retries: 10

  app:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/app
    ports:
      - "81NN:81NN"

volumes:
  pgdata:
```

Notes:

- The top-level `name:` keeps container, network, and volume names unique per
  case study.
- The volume mount path depends on the Postgres version: `/var/lib/postgresql`
  for 18 and newer, `/var/lib/postgresql/data` for 17 and older. Use the right
  one for the version you picked.
- Inside the compose network the app reaches the database at `postgres:5432`
  (service name, container port), which is why `SPRING_DATASOURCE_URL` overrides
  the `localhost:54NN` default from `application.yml`. The default stays as is
  so running from the IDE keeps working.
- Two ways to run, both documented in the case study README:
  - Everything in Docker: `docker compose up --build`
  - Development: `docker compose up -d postgres`, then run the app from the IDE
    or `./mvnw spring-boot:run` for a fast debug loop.

## Step 5: Configure the application

Replace `application.properties` with `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: <ARTIFACT_ID>
  datasource:
    url: jdbc:postgresql://localhost:54NN/app
    username: app
    password: app
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

server:
  port: 81NN

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

`ddl-auto: validate` is deliberate: Flyway owns the schema, Hibernate only checks
that entities match it.

Create `src/main/resources/db/migration/V1__init.sql` with a one-line comment
placeholder so Flyway has a migration folder to scan. Add real tables only when
the problem's domain is implemented.

## Step 6: Lay out the packages

Every case study uses strict hexagonal architecture (ports and adapters) with
DDD tactical patterns. Create this structure under the base package, with a
`package-info.java` in each leaf package so the layout exists in git before any
class does (don't generate empty placeholder classes):

```
<base package>/
├── domain/                     the model; pure Java
│   ├── model/                  aggregates, entities, value objects
│   ├── event/                  domain events
│   ├── service/                domain services (logic spanning aggregates)
│   └── exception/              domain exceptions
├── application/                use cases; orchestrates the domain
│   ├── port/
│   │   ├── in/                 use case interfaces + their command/query objects
│   │   └── out/                interfaces the application needs (repositories, gateways, clock)
│   └── service/                use case implementations
├── adapter/
│   ├── in/
│   │   └── api/                the REST API layer
│   │       ├── controller/     @RestController classes, one per aggregate/resource
│   │       ├── dto/            request/response records
│   │       ├── mapper/         DTO <-> command/domain mapping
│   │       └── error/          @RestControllerAdvice translating exceptions to responses
│   └── out/
│       └── persistence/        JPA entities, Spring Data repositories, mappers, port implementations
└── config/                     Spring wiring: creates application/domain service beans
```

### Dependency rules

Dependencies point inward only: `adapter` → `application` → `domain`.

- `domain` depends on nothing but the JDK. No Spring, no JPA, no Jackson, no
  validation annotations. Lombok is acceptable because it disappears at compile
  time. This is what makes the domain testable with plain unit tests and is the
  whole reason for the architecture, so don't relax it for convenience.
- `application` depends only on `domain`. It talks to the outside world solely
  through `port.out` interfaces. Its services are plain classes instantiated as
  beans in `config`, not annotated with `@Service`. The one allowed framework
  import is Spring's `@Transactional` on use case implementations, since the use
  case is the transaction boundary.
- `adapter.in.*` calls `port.in` interfaces, never application services or
  repositories directly. Controllers map DTO → command, call the use case, map
  result → DTO. Request/response DTOs never reach the domain.
- `adapter.out.*` implements `port.out` interfaces. JPA entities live here and
  are separate classes from domain aggregates, with an explicit mapper between
  them. Yes, this is more code than annotating the aggregate with `@Entity`;
  it's the price of a persistence-ignorant domain.
- Adapters never depend on each other.
- New technology (Redis, Kafka, an HTTP client) always arrives as a new
  `adapter.out.<tech>` package behind a `port.out` interface.

### API layer rules

The API layer is the inbound adapter at `adapter.in.api`. It is the only place
HTTP exists; nothing inward knows about status codes, JSON, or URLs.

- Endpoints live under `/api/v1/<plural-resource>` (`/api/v1/payments`). Use
  nouns for resources and HTTP verbs for actions; a domain action that isn't
  CRUD becomes a sub-resource (`POST /api/v1/payments/{id}/capture`).
- A controller method does three things only: map request DTO → command, call
  one `port.in` use case, map the result → response DTO. No business logic, no
  repository access, no `@Transactional`.
- Request and response DTOs are `record`s in `dto/`, with Bean Validation
  annotations on requests (`@Valid` on the controller parameter). This is
  input-shape validation only; business invariants stay in the domain.
- Never return or accept domain objects or JPA entities in a controller
  signature. Responses expose plain values (`String`, `BigDecimal`), not value
  objects.
- Status codes: `201` + `Location` header for creation, `200` for reads and
  updates with a body, `204` for no body, `202` when work is accepted but
  asynchronous.
- One `@RestControllerAdvice` in `error/` maps exceptions to RFC 9457
  `ProblemDetail` responses: validation failures → `400`, domain "not found" →
  `404`, domain rule violations / conflicts → `409` or `422`, anything else →
  `500` without leaking internals. Domain exceptions stay HTTP-unaware; the
  mapping lives here.

At scaffold time, create the four packages and the `@RestControllerAdvice` with
the validation and generic fallback handlers. Add controllers when the use
cases they call exist.

### DDD rules for the domain

- Name everything in the problem's ubiquitous language (`Payment.capture()`,
  not `PaymentManager.updateStatus()`).
- Aggregates protect their own invariants: state changes go through intention-
  revealing methods, no public setters, creation through a factory method or
  constructor that validates.
- Value objects are immutable `record`s that validate in the compact
  constructor (`Money`, `IdempotencyKey`). Wrap identifiers in typed IDs
  (`PaymentId`) instead of passing raw `UUID`/`Long`.
- One repository port per aggregate root, expressed in domain terms
  (`save`, `findById`), not per table.
- Aggregates reference other aggregates by ID, not by object reference.
- One transaction modifies one aggregate. Cross-aggregate effects go through
  domain events.
- Business rules live in the domain. Application services only load, call the
  domain, save, and publish; if one contains an `if` about business state, that
  logic belongs in an aggregate or domain service.

### Enforce it with ArchUnit

Rules that aren't tested erode. Add ArchUnit to the `pom.xml` (not available on
Initializr; look up the latest version on Maven Central):

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version><!-- latest --></version>
    <scope>test</scope>
</dependency>
```

And create `src/test/java/<base package path>/ArchitectureTest.java`:

```java
@AnalyzeClasses(packages = "<BASE_PACKAGE>", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_depends_on_nothing = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapter..", "..config..",
                    "org.springframework..", "jakarta.persistence..",
                    "jakarta.validation..", "com.fasterxml.jackson..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_does_not_know_adapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..config..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule inbound_adapters_use_only_input_ports = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.service..", "..application.port.out..", "..adapter.out..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule adapters_are_isolated = slices()
            .matching("..adapter.(*).(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true);
}
```

`allowEmptyShould(true)` keeps the rules passing on the empty scaffold.

Do not implement the case study's solution during scaffolding unless the user
asked for it. The scaffold is the empty, runnable starting point. When you do
implement, work inside-out: domain model and its unit tests first, then ports
and use cases, then adapters.

## Step 7: Wire the integration test to Testcontainers

Make the generated context-load test run against a real Postgres container using
`@ServiceConnection`, so tests don't depend on docker-compose being up:

```java
@SpringBootTest
@Testcontainers
class ApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:<PG_MAJOR>-alpine");

    @Test
    void contextLoads() {
    }
}
```

If Initializr already generated a `TestcontainersConfiguration` class, use that
instead of duplicating the container definition, and just align its image tag
with the compose file. Import paths for Testcontainers classes differ between
major versions, so follow what the generated code uses.

## Step 8: Write the case study README

Create `<NN-slug>/README.md` with this structure:

```markdown
# NN - <Title>

## Problem
What goes wrong, in a concrete scenario, and why it matters in production.

## Approach
The technique(s) this case study demonstrates. (Fill in as it's built.)

## Domain model
Aggregates, value objects, and domain events, in the ubiquitous language.
Which ports exist and which adapters implement them.

## Run it
Only Docker is required:

    docker compose up --build

Stop with `docker compose down` (add `-v` to also wipe the database).

For development (JDK 21 required), run only the database in Docker:

    docker compose up -d postgres
    ./mvnw spring-boot:run

App: http://localhost:81NN  |  Postgres: localhost:54NN (app/app/app)

## API
Table of endpoints: method, path, purpose.

## Try it
Copy-pasteable `curl` commands that reproduce the problem and show the fix,
with the expected response under each, so no Java knowledge is needed to test.

## Notes and trade-offs
What this solution costs, and alternatives.
```

Write the Problem section properly from what the user described. Leave Approach,
Domain model, API, Try it, and Notes as short TODO lines if nothing is implemented yet, rather than
inventing content.

## Step 9: Update the repo index

If the root `README.md` has a list or table of case studies, add a row for the
new one (number, title, one-line problem, link to the folder). If the root
README doesn't exist, create a minimal one with a title, one sentence about the
repo, and the table.

## Step 10: Register the pom as a Maven project and compile

Because there is no root `pom.xml`, IntelliJ IDEA does not notice a new case
study on its own; its `pom.xml` has to be registered (the equivalent of
right-click → "Add as Maven Project"). Do this for the user every time.

1. **Register the pom.** If a `.idea/` folder exists at the repo root, open
   `.idea/misc.xml` and add the new pom to the `MavenProjectsManager`
   component's `originalFiles` list, keeping every entry already there:

   ```xml
   <component name="MavenProjectsManager">
     <option name="originalFiles">
       <list>
         <option value="$PROJECT_DIR$/01-existing-case/pom.xml" />
         <option value="$PROJECT_DIR$/<NN-slug>/pom.xml" />
       </list>
     </option>
   </component>
   ```

   If the component doesn't exist yet, add it inside `<project>`. If `misc.xml`
   doesn't exist but `.idea/` does, create it:

   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <project version="4">
     <component name="MavenProjectsManager">
       <option name="originalFiles">
         <list>
           <option value="$PROJECT_DIR$/<NN-slug>/pom.xml" />
         </list>
       </option>
     </component>
   </project>
   ```

   If there is no `.idea/` folder (project not opened in IntelliJ yet, or a
   different IDE), skip the file edit and tell the user to right-click the new
   `pom.xml` → "Add as Maven Project" (VS Code picks up the pom automatically).

2. **Compile.** From inside the new folder:

   ```bash
   ./mvnw -B clean compile
   ```

   This downloads the dependencies and must end with `BUILD SUCCESS`. Fix any
   error before moving on; never report a scaffold that doesn't compile.

In the final report, tell the user the pom was registered and that IntelliJ may
need "Reload All Maven Projects" (Maven tool window) to show it.

## Step 11: Verify before reporting done

From inside the new folder:

```bash
docker compose up -d --wait postgres
./mvnw -q verify
docker compose up -d --build --wait
curl -fsS http://localhost:81NN/actuator/health
docker compose down
```

All must succeed. The last three prove the Docker-only path works: the image
builds, the container starts, connects to Postgres, and reports `UP`. `verify` proves the project compiles, Flyway runs, and the
Spring context starts against Postgres. If Docker isn't available in the current
environment, run `./mvnw -q -DskipTests package` at minimum and tell the user
plainly which checks were skipped.

Then report briefly: folder name, Java / Spring Boot / Postgres versions used,
the two ports, and the commands to run it.