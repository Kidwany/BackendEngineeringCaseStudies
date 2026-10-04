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

With a JDK 21 on the machine — Postgres in Docker, app on the host, which is the
fast debug loop:

    docker compose up -d
    ./mvnw spring-boot:run

Without a JDK or Maven on the machine — everything in Docker, built by the
multi-stage `Dockerfile`:

    docker compose --profile app up -d --build

The `app` service sits behind a compose profile, so the plain `docker compose up -d`
above still starts Postgres only and leaves port 8101 free for `./mvnw`. Pick one
or the other; both bind 8101.

Stop either with `docker compose --profile app down` (add `-v` to drop the
Postgres volume).

App: http://localhost:8101  |  Postgres: localhost:5401 (app/app/app)

## API

TODO — method, path, purpose.

## Try it

TODO — requests that reproduce the double execution, and the same requests
against the fixed path.

## Notes and trade-offs

TODO — what the chosen approach costs, and the alternatives it was picked over.
