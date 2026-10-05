# Distributed Lock + Fencing Tokens — Implementation Checklist

Use this file as the execution checklist for the case study.

Mark each item when it is actually completed and verified.

---

## Phase 1 — Project Bootstrap

- [x] Create Java 21+ Maven project.
- [x] Add Spring Boot.
- [x] Add Spring Web.
- [x] Add Spring Data JPA.
- [x] Add PostgreSQL driver.
- [x] Add Redis support.
- [x] Add Flyway.
- [x] Add JUnit 5 / Spring Boot Test.
- [x] Add Docker Compose.
- [x] Configure PostgreSQL container.
- [x] Configure Redis container.
- [x] Verify PostgreSQL starts successfully.
- [x] Verify Redis starts successfully.
- [x] Verify Spring Boot application starts successfully.
- [x] Configure Hibernate schema validation.
- [x] Disable Hibernate schema auto-generation as schema owner.

---

## Phase 2 — Architecture Skeleton

- [ ] Create settlement bounded context/package.
- [ ] Create domain layer/package.
- [ ] Create application layer/package.
- [ ] Create infrastructure layer/package.
- [ ] Create web adapter layer/package.
- [ ] Verify domain has no Spring dependencies.
- [ ] Verify domain has no Redis dependencies.
- [ ] Verify domain has no JPA/Hibernate dependencies where avoidable.
- [ ] Verify infrastructure depends inward through ports.
- [ ] Verify controller contains no settlement business logic.

---

## Phase 3 — Domain Model

- [ ] Create Merchant domain model or identifier type.
- [ ] Create Transaction domain model.
- [ ] Add Transaction status.
- [ ] Support `UNSETTLED`.
- [ ] Support `SETTLED`.
- [ ] Create SettlementBatch domain model.
- [ ] Create SettlementItem model.
- [ ] Add Worker ID to settlement execution data.
- [ ] Add execution strategy name to SettlementBatch.
- [ ] Leave fencing token nullable or optional for now.
- [ ] Add relevant domain behavior.
- [ ] Avoid creating an anemic controller-driven model.
- [ ] Add focused domain tests.

---

## Phase 4 — Flyway Database Schema

- [ ] Create `V1__initial_schema.sql`.
- [ ] Create merchants table.
- [ ] Create transactions table.
- [ ] Create settlement_batches table.
- [ ] Create settlement_items table.
- [ ] Add foreign keys where appropriate.
- [ ] Add useful indexes.
- [ ] Ensure `settlement_items.transaction_id` is NOT globally unique.
- [ ] Verify the same transaction ID can appear in two settlement batches.
- [ ] Start application and confirm Flyway migration succeeds.
- [ ] Confirm Hibernate schema validation succeeds.

---

## Phase 5 — Deterministic Seed Data

- [ ] Create Merchant 123.
- [ ] Create TX-1 = 100 USD.
- [ ] Create TX-2 = 200 USD.
- [ ] Create TX-3 = 300 USD.
- [ ] Set all three transactions to `UNSETTLED`.
- [ ] Confirm expected total is 600 USD.
- [ ] Query PostgreSQL manually and verify seed data exists.
- [ ] Ensure seed process is repeatable.

---

## Phase 6 — Persistence Ports

- [ ] Create transaction repository port.
- [ ] Create settlement repository port.
- [ ] Create settlement-item repository port if needed.
- [ ] Create query port for scenario inspection.
- [ ] Keep Spring Data repositories inside infrastructure.
- [ ] Implement JPA persistence adapters.
- [ ] Add mapping between domain objects and persistence entities where appropriate.
- [ ] Add repository integration tests.
- [ ] Verify persisted settlements can be read back from PostgreSQL.

---

## Phase 7 — Redis Distributed Lock

- [ ] Define distributed-lock port.
- [ ] Create Redis implementation.
- [ ] Use lock key format based on merchant ID.
- [ ] Example key: `settlement:merchant:123`.
- [ ] Make TTL configurable.
- [ ] Set demo TTL to 10 seconds.
- [ ] Ensure lock ownership has a unique owner value.
- [ ] Ensure release does not accidentally delete another worker's lock.
- [ ] Add Redis integration test.
- [ ] Verify Worker A can acquire the lock.
- [ ] Verify Worker B cannot acquire the lock before TTL expiry.
- [ ] Verify Worker B can acquire the lock after TTL expiry.

---

## Phase 8 — Execution Strategy Abstraction

- [ ] Create `SettlementExecutionStrategy`.
- [ ] Implement `NoLockSettlementExecutionStrategy`.
- [ ] Implement `RedisLockSettlementExecutionStrategy`.
- [ ] Keep Redis details out of application orchestration.
- [ ] Make strategy selectable by the scenario runner.
- [ ] Do NOT implement fencing tokens yet.
- [ ] Do NOT implement lock renewal yet.

---

## Phase 9 — Settlement Application Flow

- [ ] Create settlement use case/application service.
- [ ] Load unsettled merchant transactions.
- [ ] Calculate settlement total.
- [ ] Create SettlementBatch.
- [ ] Create SettlementItem rows.
- [ ] Mark transactions settled where appropriate.
- [ ] Persist the result.
- [ ] Record Worker ID.
- [ ] Record execution strategy.
- [ ] Add structured logs.
- [ ] Ensure the full 15-second worker pause is NOT inside one DB transaction.

---

## Phase 10 — Broken Scenario Runner

- [ ] Create Worker A.
- [ ] Create Worker B.
- [ ] Run workers independently/concurrently.
- [ ] Worker A starts first.
- [ ] Worker A acquires merchant lock.
- [ ] Worker A loads the target transactions.
- [ ] Worker A pauses for approximately 15 seconds.
- [ ] Confirm Worker A remains alive.
- [ ] Confirm Worker A does not know its lock expired.
- [ ] Confirm the Redis lease expires after approximately 10 seconds.
- [ ] Start Worker B after lock expiry.
- [ ] Worker B acquires the same merchant lock.
- [ ] Worker B processes the settlement.
- [ ] Worker B persists settlement batch.
- [ ] Worker B persists settlement items.
- [ ] Worker B completes.
- [ ] Worker A resumes.
- [ ] Worker A continues its stale execution.
- [ ] Worker A persists another business side effect.
- [ ] Verify no hidden protection accidentally prevents the failure.

---

## Phase 11 — Persisted Failure Verification

- [ ] Query `settlement_batches`.
- [ ] Confirm two settlement batches exist.
- [ ] Query `settlement_items`.
- [ ] Confirm TX-1 appears in more than one settlement.
- [ ] Confirm TX-2 appears in more than one settlement.
- [ ] Confirm TX-3 appears in more than one settlement.
- [ ] Confirm duplicate evidence exists in PostgreSQL, not only logs.
- [ ] Confirm the scenario demonstrates stale-worker execution.

Expected broken result:

```text
Settlements Created: 2
Settlement Item Rows: 6
Unique Transactions: 3
Duplicate Transaction Executions: 3
```

---

## Phase 12 — Scenario Query Side

- [ ] Create `GetScenarioResultQuery`.
- [ ] Create query for settlement history.
- [ ] Create query for settlement items.
- [ ] Create duplicate-transaction query.
- [ ] Calculate settlement count from PostgreSQL.
- [ ] Calculate unique transaction count from PostgreSQL.
- [ ] Calculate duplicate execution count from PostgreSQL.
- [ ] Ensure result queries do not depend on in-memory scenario state.
- [ ] Add tests for duplicate detection.

---

## Phase 13 — Web API

- [ ] Add endpoint to run broken scenario.
- [ ] Example: `POST /api/scenarios/distributed-lock-expiration`.
- [ ] Add endpoint to retrieve scenario result.
- [ ] Add endpoint to retrieve settlements.
- [ ] Add endpoint to retrieve duplicate transactions.
- [ ] Add endpoint to reset scenario.
- [ ] Keep controller thin.
- [ ] Verify all returned scenario evidence is queried from PostgreSQL.

---

## Phase 14 — Web UI

- [ ] Create simple scenario page.
- [ ] Add execution-strategy selector.
- [ ] Add `Run Scenario` button.
- [ ] Add `Reset Scenario` button.
- [ ] Display scenario summary.
- [ ] Display settlement batches.
- [ ] Display settlement items under each batch.
- [ ] Highlight duplicated transactions.
- [ ] Show `DUPLICATE` badge/marker.
- [ ] Show Worker ID for each settlement.
- [ ] Show execution strategy.
- [ ] Show fencing token column even if empty for current phase.
- [ ] Show created/completed timestamps.
- [ ] Show failure status when duplicates exist.
- [ ] Verify refreshing the page still shows results from PostgreSQL.

---

## Phase 15 — Structured Timeline Logging

- [ ] Log Worker A lock acquisition.
- [ ] Log Worker A pause start.
- [ ] Log expected lock expiration time.
- [ ] Log Worker B lock acquisition.
- [ ] Log Worker B settlement creation.
- [ ] Log Worker B completion.
- [ ] Log Worker A resume.
- [ ] Log Worker A stale settlement creation.
- [ ] Include merchant ID.
- [ ] Include worker ID.
- [ ] Include settlement ID.
- [ ] Include transaction IDs.
- [ ] Include timestamps.
- [ ] Verify logs allow reconstruction of the full scenario.

---

## Phase 16 — Reset Scenario

- [ ] Implement `ResetScenarioUseCase`.
- [ ] Delete settlement items.
- [ ] Delete settlement batches.
- [ ] Reset TX-1 to `UNSETTLED`.
- [ ] Reset TX-2 to `UNSETTLED`.
- [ ] Reset TX-3 to `UNSETTLED`.
- [ ] Clear scenario Redis lock key.
- [ ] Restore deterministic starting state.
- [ ] Verify reset is safe to run repeatedly.
- [ ] Verify reset does not require dropping the database.
- [ ] Verify UI becomes clean after reset.

---

## Phase 17 — Broken Scenario Final Verification

Before implementing fencing tokens, verify all of these:

- [ ] Redis distributed lock is genuinely being used.
- [ ] Worker A legitimately acquires it first.
- [ ] Worker A lease genuinely expires.
- [ ] Worker B legitimately becomes the new lock owner.
- [ ] Worker A resumes without knowing ownership was lost.
- [ ] Worker A still performs a persisted write.
- [ ] PostgreSQL contains duplicate settlement evidence.
- [ ] UI reads duplicates from PostgreSQL.
- [ ] UI highlights duplicated transactions.
- [ ] Reset returns the system to the same initial state.
- [ ] Scenario can be reproduced multiple times.

STOP HERE before implementing the solution.

---

# Phase 18 — Understand the Failure

Do not code the fencing solution until the existing implementation is understood.

- [ ] Trace Worker A execution from controller/use case to Redis.
- [ ] Trace Worker A persistence flow.
- [ ] Identify exactly when Worker A loses ownership.
- [ ] Confirm the worker receives no automatic notification that its lease expired.
- [ ] Identify the stale write boundary.
- [ ] Identify which component currently trusts stale Worker A.
- [ ] Explain why the Redis lock alone cannot stop the stale worker.
- [ ] Explain the difference between lock ownership and authorization to mutate the resource.
- [ ] Document the failure in README or engineering notes.

---

# Phase 19 — Fencing Token Design

Only start this phase after the broken scenario is fully understood.

- [ ] Define fencing-token semantics.
- [ ] Ensure every new lock ownership receives a monotonically increasing token.
- [ ] Decide where the monotonic counter lives.
- [ ] Define how the token is passed through the settlement execution.
- [ ] Define which persistent resource validates the token.
- [ ] Define how stale tokens are rejected atomically.
- [ ] Decide how the latest accepted fencing token is persisted.
- [ ] Ensure fencing validation cannot be bypassed by stale workers.
- [ ] Document expected token sequence.

Example:

```text
Worker A -> Token 41
lease expires
Worker B -> Token 42
Worker B write -> accepted
Worker A write with 41 -> rejected
```

---

## Phase 20 — Implement Fencing Token Infrastructure

- [ ] Add fencing-token generation.
- [ ] Ensure tokens are monotonic.
- [ ] Update lock acquisition result to return fencing token.
- [ ] Add fencing token to execution context.
- [ ] Persist fencing token on settlement execution where useful.
- [ ] Add required database field/state for last accepted token.
- [ ] Add Flyway migration for fencing-token schema changes.
- [ ] Keep migration backward-safe.
- [ ] Add tests for monotonic token generation.

---

## Phase 21 — Implement Fencing-Aware Strategy

- [ ] Implement `RedisLockWithFencingTokenSettlementExecutionStrategy`.
- [ ] Do not modify the broken Redis-lock strategy.
- [ ] Keep both strategies available for comparison.
- [ ] Pass fencing token through the application flow.
- [ ] Validate token at the resource mutation boundary.
- [ ] Reject stale writes.
- [ ] Record rejected stale attempt for scenario inspection.
- [ ] Add tests for stale token rejection.

---

## Phase 22 — Rerun the Exact Same Scenario

Do not change worker timing.

- [ ] Reset scenario.
- [ ] Select `Redis Lock + Fencing Token`.
- [ ] Worker A acquires lock.
- [ ] Worker A receives older fencing token.
- [ ] Worker A pauses.
- [ ] Worker A lease expires.
- [ ] Worker B acquires lock.
- [ ] Worker B receives newer fencing token.
- [ ] Worker B completes.
- [ ] Worker A resumes.
- [ ] Worker A attempts stale write.
- [ ] Stale write is rejected.

---

## Phase 23 — Verify Correct Persisted State

Expected result:

```text
Settlements Created: 1
Settlement Item Rows: 3
Unique Transactions: 3
Duplicate Transaction Executions: 0
```

Verify:

- [ ] Only one valid settlement batch exists.
- [ ] TX-1 appears only once.
- [ ] TX-2 appears only once.
- [ ] TX-3 appears only once.
- [ ] No duplicate settlement items exist.
- [ ] Worker A stale write is recorded as rejected.
- [ ] Worker B execution is recorded as successful.
- [ ] Result is verified directly from PostgreSQL.

---

## Phase 24 — Before / After Comparison UI

- [ ] Add strategy comparison view.
- [ ] Show Redis Lock Only result.
- [ ] Show Redis Lock + Fencing Token result.
- [ ] Show settlement count.
- [ ] Show settlement-item count.
- [ ] Show duplicate count.
- [ ] Show fencing token values.
- [ ] Show stale worker rejection.
- [ ] Ensure values are calculated from persisted data.
- [ ] Do not hard-code expected numbers.

---

## Phase 25 — Trade-Off Analysis

Document the answers to these questions:

- [ ] What does Redis distributed locking guarantee?
- [ ] What does it not guarantee?
- [ ] Why does TTL create stale-worker risk?
- [ ] Would lock renewal eliminate the problem completely?
- [ ] What happens during long GC pauses?
- [ ] What happens during network partitions?
- [ ] Why is fencing stronger than simply checking "do I still own the lock?"
- [ ] Where must fencing-token validation happen?
- [ ] What if the downstream resource cannot understand fencing tokens?
- [ ] What if the side effect is an external bank API?
- [ ] What are the operational costs of fencing?
- [ ] When is a DB-level atomic guard simpler than a distributed lock?
- [ ] When would idempotency still be necessary?
- [ ] Can fencing tokens replace business idempotency?
- [ ] What guarantees remain impossible across an external non-cooperating system?

---

## Phase 26 — Repository Quality

- [ ] Add clear root README.
- [ ] Add architecture diagram.
- [ ] Add broken-scenario sequence diagram.
- [ ] Add fencing-token sequence diagram.
- [ ] Document how to run Docker dependencies.
- [ ] Document how to run Spring Boot.
- [ ] Document how to run the scenario.
- [ ] Document how to reset the scenario.
- [ ] Document expected broken result.
- [ ] Document expected fencing result.
- [ ] Add screenshots of UI if useful.
- [ ] Remove dead code.
- [ ] Remove generated noise.
- [ ] Ensure secrets are not committed.
- [ ] Add `.env.example` if configuration requires environment variables.
- [ ] Ensure repository can be cloned and run by another developer.

---

## Phase 27 — LinkedIn / Case Study Readiness

Do not publish until:

- [ ] Broken scenario can be reproduced reliably.
- [ ] Fixed scenario can be reproduced reliably.
- [ ] Database evidence is visible.
- [ ] UI comparison is understandable without reading the code.
- [ ] Failure timeline is clear.
- [ ] Architecture is defensible.
- [ ] Trade-offs are documented.
- [ ] README explains why a distributed lock alone is insufficient.
- [ ] The post can show a concrete before/after result.
- [ ] The case study explains why fencing tokens solve stale ownership rather than presenting them as a magic pattern.

Suggested core message:

```text
A distributed lock can expire while the original worker is still alive.

The interesting problem is not reacquiring the lock.

The interesting problem is preventing a stale owner from continuing to mutate the protected resource.
```
