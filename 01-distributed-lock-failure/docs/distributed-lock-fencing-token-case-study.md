# Distributed Lock + Fencing Tokens Case Study

## Goal

Build a production-structured Java/Spring case study that demonstrates a subtle distributed-systems failure:

> A distributed lock with a TTL does not necessarily guarantee that only one worker can continue modifying a protected resource.

The first version must intentionally reproduce the broken scenario using a Redis distributed lock **without fencing tokens**.

Then, in a later implementation step, the exact same scenario will be rerun using **Redis Lock + Fencing Token** so the persisted database state can be compared before and after the fix.

---

# Technology Stack

Use:

- Java 21+
- Spring Boot
- Spring Data JPA
- Hibernate
- PostgreSQL
- Redis
- Flyway
- Docker Compose
- Maven
- JUnit 5

## Database Ownership

Use Flyway for all schema migrations.

Do not use Hibernate automatic schema creation as the source of truth.

Recommended setting:

```text
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate should validate the schema while Flyway owns migrations.

---

# Architecture

Use:

- Domain-Driven Design
- Hexagonal Architecture
- Ports and Adapters
- Dependency inversion
- Clear separation between domain, application, and infrastructure concerns

The domain must not depend on:

- Spring
- Redis
- PostgreSQL
- Hibernate
- HTTP
- infrastructure implementations

A possible package layout:

```text
settlement
├── domain
│   ├── model
│   ├── service
│   └── port
├── application
│   ├── command
│   ├── query
│   ├── service
│   └── port
├── infrastructure
│   ├── persistence
│   ├── redis
│   ├── locking
│   └── configuration
└── adapter
    └── web
```

Do not follow this structure mechanically if a cleaner DDD/Hexagonal organization is more appropriate.

The important rule is that dependencies point inward.

---

# Business Scenario

The system processes merchant settlements.

A merchant has multiple financial transactions that have not yet been settled.

A settlement worker:

1. Acquires a distributed lock for a merchant.
2. Loads unsettled transactions.
3. Performs settlement processing.
4. Creates a settlement batch.
5. Associates the transactions with the settlement.
6. Marks the transactions as settled.
7. Releases the distributed lock.

There may be multiple worker instances running concurrently.

Only one worker should logically process settlement for the same merchant at a time.

---

# Domain Model

Keep the domain intentionally small but meaningful.

Possible entities/aggregates:

```text
Merchant
Transaction
SettlementBatch
SettlementItem
```

## Transaction

Possible fields:

```text
id
merchantId
amount
currency
status
createdAt
updatedAt
```

Possible statuses:

```text
UNSETTLED
SETTLED
```

## SettlementBatch

Possible fields:

```text
id
merchantId
workerId
executionStrategy
fencingToken
totalAmount
createdAt
completedAt
```

## SettlementItem

This is important for preserving evidence of duplicate processing.

Possible fields:

```text
id
settlementBatchId
transactionId
amount
currency
processedAt
```

The same business transaction ID must be allowed to appear in more than one settlement batch during the intentionally broken scenario.

Do **not** add a global unique constraint on `transaction_id` in `settlement_items` yet.

---

# Database Schema

Use PostgreSQL.

Create the initial schema through Flyway.

Suggested first migration:

```text
V1__initial_schema.sql
```

Suggested tables:

```text
merchants
transactions
settlement_batches
settlement_items
```

Seed a deterministic dataset for one merchant.

Example:

```text
Merchant: 123

TX-1 = 100 USD
TX-2 = 200 USD
TX-3 = 300 USD
```

Expected total settlement:

```text
600 USD
```

The seed data must make the scenario repeatable.

---

# Why SettlementItem Exists

Do not rely only on the `transactions` table to prove that duplicate processing occurred.

If a transaction has only one `settlement_id`, then one worker may overwrite another worker's result and the duplicate execution evidence may disappear.

Instead:

```text
transactions
```

represents the current business state.

And:

```text
settlement_items
```

represents the immutable or append-only evidence that a worker processed a transaction as part of a settlement batch.

This allows the database to prove that:

```text
TX-1
```

appeared in:

```text
Settlement #101
Settlement #102
```

during the broken scenario.

---

# Distributed Lock

Implement Redis-based distributed locking.

The lock key should be conceptually similar to:

```text
settlement:merchant:{merchantId}
```

Use a configurable TTL.

For the demonstration:

```text
TTL = 10 seconds
```

The Redis implementation must be behind a port.

Example:

```java
public interface DistributedLock {
    LockHandle tryAcquire(String resource, Duration ttl);
}
```

The exact interface may differ if a better design is appropriate.

The application layer must not depend directly on Redis.

---

# Settlement Execution Strategy

Create an abstraction so multiple implementations can later be compared.

Example:

```java
public interface SettlementExecutionStrategy {
    SettlementResult execute(MerchantId merchantId);
}
```

Potential strategies:

```text
NoLockSettlementExecutionStrategy
RedisLockSettlementExecutionStrategy
RedisLockWithRenewalSettlementExecutionStrategy
RedisLockWithFencingTokenSettlementExecutionStrategy
```

For the initial broken version implement only:

```text
NoLockSettlementExecutionStrategy
RedisLockSettlementExecutionStrategy
```

Do not implement fencing tokens yet.

Do not implement automatic lock renewal yet unless required by the chosen library.

---

# Broken Scenario

Create two independent logical workers:

```text
Worker A
Worker B
```

They may execute inside one application process for simplicity, but must behave like independent workers.

The scenario should be deterministic enough to reproduce repeatedly.

## Worker A

At approximately:

```text
T = 0
```

Worker A:

1. Acquires the Redis lock for Merchant 123.
2. Uses a 10-second lock TTL.
3. Loads the unsettled transactions.
4. Starts settlement processing.
5. Pauses long enough for the lease to expire.

Simulate something conceptually similar to:

- long GC pause
- CPU starvation
- VM pause
- thread suspension
- temporary process freeze

For the demo, an artificial blocking pause is acceptable.

Example:

```text
Worker A pause = 15 seconds
```

Worker A must remain alive.

It must not know that the lease expired.

When the pause ends, Worker A resumes its original workflow.

## Worker B

Worker B begins after Worker A's lock has expired.

Example:

```text
T = 11 seconds
```

Worker B:

1. Attempts to acquire the same merchant lock.
2. Acquires it successfully.
3. Processes the same business data.
4. Creates a settlement batch.
5. Creates settlement items.
6. Marks transactions as settled.
7. Completes normally.

Worker A then resumes and continues performing its stale writes.

---

# Critical Requirement

The first implementation is intentionally broken.

Do not accidentally prevent the failure with:

- optimistic locking
- pessimistic locking
- serializable isolation
- unique constraints that block duplicates
- fencing tokens
- idempotency guards
- automatic conflict rejection

The scenario must prove:

```text
Worker A once legitimately owned the lock.

Worker A's lease expired.

Worker B became the new valid owner.

Worker A later resumed.

Worker A still performed a persisted business side effect.
```

---

# Transaction Boundaries

Use local database transactions where appropriate.

Be explicit about transaction boundaries.

Do not keep one database transaction open during the simulated 15-second pause.

The experiment is about stale distributed-lock ownership, not long-running database locks.

---

# Database-Driven Visualization

The web interface must read the final scenario result from PostgreSQL.

Do not build the final visualization purely from in-memory data or logs.

The UI should query persisted settlement data after the scenario finishes.

---

# Settlement History UI

Show all settlement batches created by the scenario.

For each settlement display:

```text
Settlement ID
Merchant ID
Worker ID
Execution Strategy
Fencing Token
Created At
Completed At
Total Amount
```

For each settlement, show its settlement items:

```text
Transaction ID
Amount
Currency
Processed At
```

---

# Duplicate Detection

The UI must detect transactions that exist in more than one settlement batch.

Example persisted result:

```text
Settlement #101
Worker B

TX-1
TX-2
TX-3
```

and:

```text
Settlement #102
Worker A

TX-1
TX-2
TX-3
```

The interface should highlight:

```text
TX-1
TX-2
TX-3
```

as duplicated.

Use a clear visual marker such as:

```text
DUPLICATE
```

The duplicate detection must come from database queries.

Do not hard-code the expected duplicated transaction IDs.

---

# Scenario Summary

Display metrics calculated from persisted data.

Example broken result:

```text
Settlements Created: 2
Settlement Item Rows: 6
Unique Transactions: 3
Duplicate Transaction Executions: 3
```

And display:

```text
FAILURE DETECTED
```

when duplicate transaction executions are found.

---

# Execution Timeline

Structured logs should capture the runtime execution timeline.

Important fields:

```text
workerId
merchantId
lockKey
lockAcquiredAt
lockExpiresAt
settlementId
transactionIds
timestamp
```

Example:

```text
00s Worker A acquired lock
00s Worker A loaded transactions
02s Worker A paused

10s Worker A lock expired

11s Worker B acquired lock
11s Worker B loaded transactions
12s Worker B created settlement
13s Worker B completed

17s Worker A resumed
18s Worker A created settlement
19s Worker A completed
```

Logs explain **how** the failure occurred.

PostgreSQL provides evidence of **what persisted**.

---

# API / Web Interface

At minimum provide:

```text
POST /api/scenarios/distributed-lock-expiration
```

A small web UI is preferred.

The UI should provide:

```text
Execution Strategy
[ Redis Lock Only ▼ ]

[ Run Scenario ]
[ Reset Scenario ]
```

For now implement only:

```text
No Lock
Redis Lock Only
```

Prepare the design so later strategies can be added without rewriting the scenario infrastructure.

---

# Scenario Reset

Provide:

```text
POST /api/scenarios/reset
```

The reset operation must restore the system to exactly the same starting state.

It should:

- delete settlement items
- delete settlement batches
- restore scenario transactions to UNSETTLED
- clear scenario-specific Redis lock keys
- reset fencing-token state when fencing is introduced later
- recreate the deterministic seed dataset if necessary

Prefer deterministic transactional cleanup instead of dropping the entire database.

---

# Lightweight Command / Query Separation

Keep scenario execution and scenario inspection separate.

Conceptually:

```text
RunScenarioUseCase
ResetScenarioUseCase
GetScenarioResultQuery
GetSettlementsQuery
GetDuplicateTransactionsQuery
```

This is intentionally lightweight.

Do not introduce a second database or full CQRS infrastructure.

The purpose is to separate:

```text
Commands:
- run scenario
- reset scenario

Queries:
- inspect settlements
- inspect duplicated transactions
- inspect final result
```

---

# Future Fencing Token Phase

The next implementation phase will add:

```text
RedisLockWithFencingTokenSettlementExecutionStrategy
```

The same scenario timing must be reused.

Do not simplify the second run.

The sequence should remain:

```text
Worker A acquires lock
Worker A pauses
Worker A lease expires
Worker B acquires newer ownership
Worker B completes
Worker A resumes
Worker A attempts a stale write
```

Only the coordination strategy should change.

Expected future database result:

```text
Settlements Created: 1
Settlement Item Rows: 3
Unique Transactions: 3
Duplicate Transaction Executions: 0
```

Worker A's stale execution should be rejected.

The UI should expose the rejected stale attempt separately, for example:

```text
Worker A
Fencing Token: 41
Result: REJECTED — STALE FENCING TOKEN

Worker B
Fencing Token: 42
Result: SUCCESS
```

---

# Before / After Comparison

Prepare the application so the final case study can demonstrate:

## Redis Lock Only

```text
Settlement batches: 2
Settlement items: 6
Duplicate transaction executions: 3

TX-1 DUPLICATE
TX-2 DUPLICATE
TX-3 DUPLICATE
```

## Redis Lock + Fencing Token

```text
Settlement batches: 1
Settlement items: 3
Duplicate transaction executions: 0

Worker A stale execution:
REJECTED
```

The comparison must be based on real persisted database data.

Do not hard-code the final result.

---

# Testing

Add useful tests only.

At minimum:

- domain tests
- JPA repository integration tests
- Redis lock integration tests
- scenario test that proves stale-worker execution can occur
- duplicate detection query test
- reset scenario test

Later add tests for fencing-token rejection.

---

# Docker Compose

Provide Docker Compose configuration for:

```text
PostgreSQL
Redis
```

The Spring Boot application may run outside Docker if that keeps the development loop simple.

Running the environment should require only a small number of commands.

---

# README Requirements

The repository README should contain:

## Problem

Explain the merchant settlement scenario.

## Architecture

Explain the DDD and Hexagonal boundaries.

## Infrastructure

Explain the roles of:

```text
Spring Boot
PostgreSQL
Redis
JPA / Hibernate
Flyway
```

## Running

Explain how to start the dependencies and application.

## Reproducing the Failure

Provide exact steps.

## Expected Broken Result

Show the expected duplicate persisted state.

## Current Limitation

State clearly:

> This version intentionally demonstrates the failure. Fencing tokens have not been implemented yet.

Do not explain or implement the final fencing-token solution in the initial version.

---

# Code Quality Requirements

Treat this as a real engineering case study, not disposable sample code.

Use:

- meaningful names
- small focused classes
- explicit boundaries
- dependency inversion
- domain types where useful
- no giant service classes
- no business logic in controllers
- no direct Redis usage inside application services
- no direct Spring Data dependency inside the domain
- no unnecessary abstractions purely to appear "DDD"

The final codebase should be easy for an experienced backend engineer to read and modify.
