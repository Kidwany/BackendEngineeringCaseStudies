# 01 - Distributed lock failure

## Problem

A job runs on a schedule and must run exactly once per tick, no matter how many
instances of the service are deployed. The usual first attempt is a lock row in
the database: read it, see that nobody holds it, write your own owner id, do the
work, delete the row.

That is a check-then-act across two statements, so it is not a lock at all. With
two instances behind a load balancer the interleaving is ordinary, not exotic:

1. Instance A reads the lock row and sees it is free.
2. Instance B reads the same row, in the same window, and also sees it is free.
3. Both write themselves as owner — the second write silently wins.
4. Both proceed into the critical section.

The job now runs twice: a payout is sent twice, a reconciliation batch
double-counts, a counter ends up lower than the number of increments because two
read-modify-write cycles overlapped and one update was lost.

The second failure mode is worse, because it survives a correct acquire. Locks
need a lease, or a crashed holder blocks the job forever. But a lease is a
deadline, and a holder that pauses — a long GC, a slow database call, the VM
being descheduled — can wake up past its own expiry, after a second holder has
legitimately acquired the lock, and keep writing as if it still owned it. Both
instances believe they are the only one in the critical section, and the lock's
own correctness guarantee is what convinced them. Clock skew between instances
widens the window further.

So "we take a lock" is not a design. What matters is whether acquisition is a
single atomic operation, what happens when the holder dies, what happens when the
holder is merely slow, and whether the resource being protected can reject a
write from a holder whose lease has already passed.

## Approach

TODO — the locking strategies this case study builds and breaks, in order.

## Domain model

TODO — aggregates, value objects, and domain events; which ports exist and which
adapters implement them.

## Run it

A Maven multi-module build with three independent Spring Boot apps:

| App | Module | Port | Depends on |
|---|---|---|---|
| Payout Platform | `payout-platform` | 8101 | `payout-postgres`, Kafka |
| Payout Worker | `payout-worker` | 8102 | Redis, Kafka, Fake Bank (HTTP) |
| Fake Bank Service | `fake-bank` | 8103 | `bank-postgres` |

Infrastructure, all in `docker-compose.yml`:

| Service | Host address | In-network address | Credentials |
|---|---|---|---|
| `payout-postgres` | localhost:5401 | payout-postgres:5432 | db/user/password `payout` |
| `bank-postgres` | localhost:5403 | bank-postgres:5432 | db/user/password `bank` |
| `redis` | localhost:6301 | redis:6379 | — |
| `kafka` (KRaft, single node) | localhost:9401 | kafka:29092 | — |

The bank is an external system, so it gets its own database rather than a schema in the
platform's. Kafka advertises two listeners: `localhost:9401` for apps run with `./mvnw`,
and `kafka:29092` for apps running in compose.

With a JDK 21 on the machine — infrastructure in Docker, apps on the host, which is the
fast debug loop:

    docker compose up -d
    ./mvnw -pl payout-platform spring-boot:run
    ./mvnw -pl payout-worker spring-boot:run
    ./mvnw -pl fake-bank spring-boot:run

Without a JDK or Maven on the machine — everything in Docker, each app built by the
shared multi-stage `Dockerfile` (compose passes the module as the `MODULE` build arg):

    docker compose --profile app up -d --build

The app services sit behind a compose profile, so the plain `docker compose up -d`
above still starts only the infrastructure and leaves ports 8101–8103 free for `./mvnw`.
Pick one or the other; both bind the same ports.

Stop either with `docker compose --profile app down` (add `-v` to drop both
Postgres volumes).

## API

TODO — method, path, purpose.

## Try it

TODO — requests that reproduce the double execution, and the same requests
against the fixed path.

## Notes and trade-offs

TODO — what the chosen approach costs, and the alternatives it was picked over.
