# Backend engineering case studies

A collection of small, self-contained Spring Boot projects, each one built around a
single backend failure mode: what breaks, why, and what the fix costs. Every case
study is an independent project with its own `pom.xml`, Maven wrapper, and
`docker-compose.yml` — `cd` into one and run it without touching the others.

| # | Case study | Problem | App | Postgres |
|---|---|---|---|---|
| 01 | [Distributed lock failure](01-distributed-lock-failure) | Two instances hold the "same" lock: non-atomic acquire, and leases that expire under a paused holder. | 8101–8103 | 5401 |

Ports are derived from the case study number (`81NN` for the app, `54NN` for
Postgres), so several can run side by side.

Each case study runs two ways: `docker compose up -d` + `./mvnw spring-boot:run`
if you have a JDK, or `docker compose --profile app up -d --build` to build and
run the app in Docker too, on a machine with no Java installed.
