# Payout Delivery Failure Lab — Implementation Checklist

Mark each item only after it is implemented and verified.

---

## Phase 1 — Repository Bootstrap

- [x] Create Java 21+ Maven repository.
- [x] Add Payout Platform application.
- [x] Add Payout Worker application.
- [x] Add Fake Bank Service application.
- [x] Add root Docker Compose.
- [x] Verify all applications compile.

## Phase 2 — Infrastructure

- [ ] Add PostgreSQL for Payout Platform.
- [ ] Add PostgreSQL for Fake Bank.
- [ ] Add Redis.
- [ ] Add Kafka.
- [ ] Verify Docker Compose networking.
- [ ] Verify both DBs start.
- [ ] Verify Redis starts.
- [ ] Verify Kafka starts.

## Phase 3 — Flyway / Hibernate

- [ ] Add Flyway to Payout Platform.
- [ ] Add Flyway to Fake Bank.
- [ ] Set Hibernate to validate schema.
- [ ] Disable Hibernate schema auto-creation as schema owner.
- [ ] Verify migrations succeed.

## Phase 4 — Hexagonal Structure

- [ ] Create domain layer.
- [ ] Create application layer.
- [ ] Create infrastructure adapters.
- [ ] Create web adapters.
- [ ] Keep Spring out of domain logic.
- [ ] Keep Redis/Kafka/HTTP/JPA concerns outside domain.

## Phase 5 — Payout Domain

- [ ] Create PayoutBatch.
- [ ] Add merchant ID.
- [ ] Add amount.
- [ ] Add currency.
- [ ] Add READY status.
- [ ] Add DISPATCHING status.
- [ ] Add DISPATCHED status.
- [ ] Add FAILED status if needed.
- [ ] Add domain tests.

## Phase 6 — Payout Platform Schema

- [ ] Create payout_batches table.
- [ ] Create outbox_events table.
- [ ] Create payout_dispatch_attempts table.
- [ ] Add indexes.
- [ ] Add required migrations.
- [ ] Verify schema in PostgreSQL.

## Phase 7 — Deterministic Data

- [ ] Seed Merchant M-1001.
- [ ] Seed Payout Batch PB-9001.
- [ ] Set amount = 252,000 EGP.
- [ ] Set status = READY.
- [ ] Ensure reset can recreate the exact same state.

## Phase 8 — Transactional Outbox

- [ ] Persist payout batch.
- [ ] Persist PayoutBatchReady event in same transaction.
- [ ] Verify atomic commit.
- [ ] Add outbox status.
- [ ] Add published timestamp.
- [ ] Add integration test.

## Phase 9 — Outbox Publisher

- [ ] Define OutboxPublisherPort.
- [ ] Implement Kafka publisher adapter.
- [ ] Poll unpublished rows.
- [ ] Publish to `payout-ready`.
- [ ] Mark published event.
- [ ] Verify Kafka receives message.

## Phase 10 — Kafka Consumer

- [x] Configure consumer group.
- [x] Consume PayoutBatchReady.
- [x] Include event ID.
- [x] Include batch ID.
- [x] Include merchant ID.
- [x] Include amount/currency.
- [x] Verify worker receives message.

## Phase 11 — Fake Bank Application

- [x] Create separate Spring Boot service.
- [x] Add REST controller.
- [x] Add JPA.
- [x] Add Flyway.
- [x] Keep service intentionally small.

## Phase 12 — Fake Bank Schema

- [x] Create bank_payouts table.
- [x] Add payout_batch_id.
- [x] Add merchant_id.
- [x] Add amount.
- [x] Add currency.
- [x] Add idempotency_key.
- [x] Add worker_id.
- [x] Add received_at.
- [x] Add status.
- [x] Do NOT enforce idempotency yet.

## Phase 13 — Fake Bank API

- [x] Add `POST /api/bank/payouts`.
- [x] Persist every request in broken mode.
- [x] Return payout ID.
- [x] Verify duplicate requests create duplicate rows.

## Phase 14 — Fake Bank Behavior Modes

- [x] Add NORMAL.
- [x] Add DELAY_RESPONSE.
- [x] Add PROCESS_THEN_DELAY_RESPONSE.
- [x] Add RETURN_500.
- [x] Add TIMEOUT.
- [x] Make delay configurable.

## Phase 15 — Bank Adapter

- [x] Define BankPayoutPort.
- [x] Implement HTTP adapter.
- [x] Send batch ID.
- [x] Send merchant ID.
- [x] Send amount/currency.
- [x] Prepare Idempotency-Key support.
- [x] Keep idempotency disabled initially.

## Phase 16 — Redis Lease

- [x] Define DistributedLeasePort.
- [x] Implement Redis adapter.
- [x] Use `payout:batch:{batchId}`.
- [x] Set TTL to 10 seconds for demo.
- [x] Use unique owner ID.
- [x] Prevent one owner from deleting another owner's lock.
- [x] Verify expiration.

## Phase 17 — Dispatch Attempt History

- [x] Persist worker ID.
- [x] Persist batch ID.
- [x] Persist strategy.
- [x] Persist lease owner.
- [x] Add nullable fencing token.
- [x] Persist started_at.
- [x] Persist lease_expires_at.
- [x] Persist validation_completed_at.
- [x] Persist bank_request_sent_at.
- [x] Persist completed_at.
- [x] Persist final status.

## Phase 18 — Broken Worker Strategy

- [x] Worker acquires lease.
- [x] Worker loads payout.
- [x] Worker validates payout.
- [x] Worker performs final ownership/state check.
- [x] Worker prepares HTTP request.
- [x] Worker can pause after final check.
- [x] Worker resumes from exact pause point.
- [x] Worker sends HTTP request without re-running validation.

## Phase 19 — Broken Scenario

- [x] Start Worker A.
- [x] Worker A acquires lease.
- [x] Worker A passes validation.
- [x] Worker A pauses after validation.
- [x] Keep A paused past lease TTL.
- [x] Confirm lease expires.
- [x] Start Worker B.
- [x] Worker B acquires lease.
- [x] Worker B calls Fake Bank.
- [x] Fake Bank persists first payout.
- [x] Worker B completes.
- [x] Resume Worker A.
- [x] Worker A sends stale prepared request.
- [x] Fake Bank persists second payout.

## Phase 20 — Verify Broken Result

- [ ] Fake Bank DB contains 2 rows for PB-9001.
- [ ] Both rows contain 252,000 EGP.
- [ ] One came from Worker B.
- [ ] One came from Worker A.
- [ ] Total actual amount = 504,000 EGP.
- [ ] Duplicate exists in persisted bank data.

## Phase 21 — Query APIs

Payout Platform:
- [ ] Get payout batch.
- [ ] Get outbox events.
- [ ] Get dispatch attempts.
- [ ] Get scenario result.

Fake Bank:
- [ ] List bank payouts.
- [ ] Get duplicate payouts.
- [ ] Get payout summary.

---

## Phase 22A — Frontend Bootstrap

- [ ] Create simple React frontend.
- [ ] Add API client layer.
- [ ] Configure Payout Platform API base URL.
- [ ] Configure Fake Bank API access if frontend calls it directly.
- [ ] Add minimal layout.
- [ ] Add case-study title.
- [ ] Add scenario subtitle.
- [ ] Avoid unnecessary UI frameworks unless useful.

## Phase 22B — Scenario Explanation

- [ ] Add "What will happen?" panel.
- [ ] Explain prepare batch step.
- [ ] Explain outbox commit.
- [ ] Explain Kafka publication.
- [ ] Explain worker competition.
- [ ] Explain Worker A pause.
- [ ] Explain lease expiration.
- [ ] Explain Worker B takeover.
- [ ] Explain stale Worker A resume.
- [ ] Explain broken vs protected expected result.
- [ ] Change explanation according to selected mode if useful.

## Phase 22C — Scenario Controls

- [ ] Add Scenario Mode selector.
- [ ] Add BROKEN option.
- [ ] Add PROTECTED option.
- [ ] Add worker-count selector.
- [ ] Support 2 workers.
- [ ] Support 3 workers.
- [ ] Support 4 workers.
- [ ] Support 5 workers.
- [ ] Default worker count to 2.
- [ ] Keep Worker A as deterministic delayed worker.

## Phase 22D — Prepare Payout Batch Action

- [ ] Add `Prepare Payout Batch` button.
- [ ] Call `POST /api/payout-batches/prepare`.
- [ ] Create/update PB-9001.
- [ ] Create PayoutBatchReady outbox event in same DB transaction.
- [ ] Do NOT publish directly from frontend.
- [ ] Show prepared payout data.
- [ ] Show outbox event data.
- [ ] Show outbox initially as PENDING.
- [ ] Disable Run Scenario until preparation succeeds.

## Phase 22E — Outbox / Kafka Visualization

- [ ] Show outbox event ID.
- [ ] Show event type.
- [ ] Show PENDING state.
- [ ] Show PUBLISHED state after publisher sends event.
- [ ] Show Kafka event received by worker.
- [ ] Ensure UI state comes from backend persisted/runtime data.

## Phase 22F — Run Scenario Action

- [ ] Add `Run Scenario` button.
- [ ] Send selected mode.
- [ ] Send worker count.
- [ ] Use deterministic PB-9001.
- [ ] Keep same amount and merchant.
- [ ] Keep same timing between broken/protected runs.
- [ ] Prevent accidental multiple simultaneous scenario runs.

## Phase 22G — Scenario Progress

- [ ] Add current scenario endpoint.
- [ ] Poll scenario status every 500–1000ms.
- [ ] Show Worker A lease acquired.
- [ ] Show Worker A validated.
- [ ] Show Worker A paused.
- [ ] Show lease expiration.
- [ ] Show Worker B lease acquired.
- [ ] Show Worker B bank request.
- [ ] Show Worker A resume.
- [ ] Show Worker A bank request.
- [ ] Stop polling when scenario completes.
- [ ] Do not add WebSockets unless later justified.

## Phase 22H — Fake Bank Operations Table

- [ ] Fetch persisted bank operations.
- [ ] Show bank payout ID.
- [ ] Show payout batch ID.
- [ ] Show worker ID.
- [ ] Show amount.
- [ ] Show idempotency key where available.
- [ ] Show received timestamp.
- [ ] Show processing result.
- [ ] Group by business payout batch.
- [ ] Detect duplicates from persisted data.
- [ ] Highlight duplicate rows.
- [ ] Add DUPLICATE badge.
- [ ] Show IDEMPOTENT_REPLAY in protected mode.

## Phase 22I — Result Summary

Broken:
- [ ] Show FAILURE DETECTED.
- [ ] Show selected worker count.
- [ ] Show bank requests attempted.
- [ ] Show actual financial payouts.
- [ ] Show duplicate payout count.
- [ ] Show intended amount.
- [ ] Show actual bank amount.

Protected:
- [ ] Show PROTECTED.
- [ ] Show selected worker count.
- [ ] Show bank requests attempted.
- [ ] Show actual financial payouts = 1.
- [ ] Show idempotent replay count.
- [ ] Show duplicate payout count = 0.
- [ ] Show actual amount = intended amount.

## Phase 22J — Clear & Reset

- [ ] Add `Clear & Reset` button.
- [ ] Call scenario reset endpoint.
- [ ] Delete payout dispatch attempts.
- [ ] Delete/reset outbox scenario data.
- [ ] Reset payout batch state.
- [ ] Delete Fake Bank payout rows.
- [ ] Delete Fake Bank request-attempt history if present.
- [ ] Clear bank idempotency records.
- [ ] Clear Redis lease keys.
- [ ] Reset fencing state.
- [ ] Ensure old Kafka messages do not contaminate next logical run.
- [ ] Clear frontend state.
- [ ] Verify next run starts cleanly.

## Phase 22K — Optional Clear Variants

- [ ] Decide whether one reset button is sufficient.
- [ ] If useful, add `Clear Results`.
- [ ] If useful, add `Reset Entire Scenario`.
- [ ] Do not add both unless distinction is actually useful.

## Phase 22L — Frontend Quality

- [ ] Keep frontend intentionally simple.
- [ ] No authentication.
- [ ] No unnecessary routing complexity.
- [ ] No Redux or large state framework unless clearly needed.
- [ ] No heavy design system requirement.
- [ ] Prefer readable code over visual polish.
- [ ] Make the case study understandable without reading source code.

## Phase 23 — Duplicate Highlighting

- [ ] Group bank payouts by payout_batch_id.
- [ ] Highlight count > 1.
- [ ] Add DUPLICATE badge.
- [ ] Show Bank Requests Received.
- [ ] Show Unique Payout Batches.
- [ ] Show Duplicate Bank Payouts.
- [ ] Show Total Intended Amount.
- [ ] Show Total Actual Amount.
- [ ] Show FAILURE DETECTED.

## Phase 24 — Reset Scenario

Payout Platform:
- [ ] Delete dispatch attempts.
- [ ] Reset PB-9001 to READY.
- [ ] Reset/recreate outbox state.
- [ ] Clear Redis lease.

Fake Bank:
- [ ] Delete bank payout rows.
- [ ] Clear idempotency state.
- [ ] Reset behavior mode.

Verification:
- [ ] Reset reproduces exact initial state.
- [ ] Broken scenario can run repeatedly.

## Phase 25 — Understand Failure Before Fix

- [ ] Trace outbox creation.
- [ ] Trace Kafka publication.
- [ ] Trace Kafka consumption.
- [ ] Trace Worker A lease.
- [ ] Identify final validation point.
- [ ] Identify pause after check.
- [ ] Confirm ownership changes while A is paused.
- [ ] Confirm A resumes after validation.
- [ ] Explain why another check is not a formal guarantee.
- [ ] Explain why Outbox does not guarantee exactly-once bank execution.
- [ ] Explain why lease alone is insufficient.

STOP here until the failure is fully understood.

## Phase 26 — Add Fencing Tokens

- [ ] Design monotonic fencing token generation.
- [ ] Add token to lease acquisition.
- [ ] Worker A gets older token.
- [ ] Worker B gets newer token.
- [ ] Persist token in dispatch attempts.
- [ ] Add Flyway migration if needed.
- [ ] Keep broken strategy unchanged.
- [ ] Add protected strategy.

## Phase 27 — Demonstrate Fencing Limitation

- [ ] Let A pass internal fencing validation.
- [ ] Pause A after fencing validation.
- [ ] Let A lease expire.
- [ ] Let B get newer token.
- [ ] Resume A.
- [ ] Confirm A can still call external HTTP if bank does not enforce fencing.
- [ ] Document: fencing protects only resources that enforce it.

## Phase 28 — Stable Business Idempotency Key

- [ ] Define `payout:PB-9001`.
- [ ] Do not use worker ID.
- [ ] Do not use Kafka event ID.
- [ ] Do not use retry ID.
- [ ] Do not use fencing token.
- [ ] Ensure A and B send the same key.

## Phase 29 — Fake Bank Idempotency

- [ ] Enforce unique business idempotency key.
- [ ] First request creates payout.
- [ ] Second request returns original result.
- [ ] Do not create second financial payout.
- [ ] Mark replay as IDEMPOTENT_REPLAY.
- [ ] Add integration tests.

## Phase 30 — Protected Scenario

Use identical inputs and timing.

- [ ] Reset scenario.
- [ ] Run PB-9001.
- [ ] Start Worker A.
- [ ] A gets older token.
- [ ] A validates.
- [ ] A pauses.
- [ ] Lease expires.
- [ ] B gets newer token.
- [ ] B calls bank.
- [ ] Bank creates payout.
- [ ] A resumes.
- [ ] A calls bank with same idempotency key.
- [ ] Bank treats it as replay.
- [ ] No second payout is created.

## Phase 31 — Verify Protected Result

- [ ] Bank calls attempted = 2.
- [ ] Actual payouts created = 1.
- [ ] Duplicate payouts = 0.
- [ ] Idempotent replays = 1.
- [ ] Total actual amount = 252,000 EGP.
- [ ] Result comes from persisted Fake Bank data.

## Phase 32 — UI Comparison

Broken:
- [ ] Show 2 actual payouts.
- [ ] Highlight duplicate.
- [ ] Show 504,000 EGP total.
- [ ] Show FAILURE DETECTED.

Protected:
- [ ] Show 1 actual payout.
- [ ] Show second request as IDEMPOTENT_REPLAY.
- [ ] Show 252,000 EGP total.
- [ ] Show duplicate count = 0.

## Phase 33 — Kafka Reliability Experiments

Only after core scenario works:

- [ ] Redeliver same Kafka event.
- [ ] Restart consumer after processing.
- [ ] Simulate consumer rebalance.
- [ ] Verify bank idempotency still protects payout.

## Phase 34 — Fake Bank Failure Experiments

Only after core scenario works:

- [ ] DELAY_RESPONSE.
- [ ] PROCESS_THEN_DELAY_RESPONSE.
- [ ] RETURN_500.
- [ ] TIMEOUT.
- [ ] Verify retries do not duplicate payout when idempotency is enabled.

## Phase 35 — Documentation

- [ ] Explain Transactional Outbox.
- [ ] Explain Kafka at-least-once behavior.
- [ ] Explain lease expiration.
- [ ] Explain stale worker.
- [ ] Explain check-then-act gap.
- [ ] Explain fencing.
- [ ] Explain fencing limitation for external APIs.
- [ ] Explain stable business idempotency.
- [ ] Add broken sequence diagram.
- [ ] Add protected sequence diagram.

## Phase 36 — Repository Quality

- [ ] No unnecessary microservices.
- [ ] No Kubernetes requirement.
- [ ] No service mesh.
- [ ] No unnecessary Kafka topics.
- [ ] Remove dead code.
- [ ] Add `.env.example` if needed.
- [ ] Verify clean clone can run project.

## Phase 37 — Publish Readiness

- [ ] Broken scenario reliably duplicates payout.
- [ ] Duplicate is visible in Fake Bank DB.
- [ ] Duplicate is visible in UI.
- [ ] Protected scenario uses identical data/timing.
- [ ] Protected scenario creates one financial payout.
- [ ] Replay is visible.
- [ ] Outbox role is documented correctly.
- [ ] Kafka role is documented correctly.
- [ ] Fencing limitation is documented correctly.
- [ ] Idempotency role is documented correctly.

Final takeaway:

```text
Outbox prevents lost publication.
Kafka may redeliver.
Leases can expire while workers are alive.
Fencing protects cooperating internal resources.
External financial side effects need business-level idempotency.
```
