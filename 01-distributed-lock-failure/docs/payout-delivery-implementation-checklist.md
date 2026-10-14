# Payout Delivery Failure Lab — Implementation Checklist

Mark each item only after it is implemented and verified.

Two branches, so the diff between them is the fix:

- **Part 1 — `master` (Phases 1–25):** reproduce the failure only. Outbox, Kafka, Redis lease, a paused worker,
  and a duplicate payout at the bank. No fencing tokens, no PROTECTED strategy, no idempotency keys.
- **Part 2 — solution branch (Phases 26–32B):** fencing tokens, the PROTECTED strategy, and bank idempotency,
  added as new migrations and code on top of `master`. Phase 32B collects the items moved out of Part 1.
- **Part 3 — both branches (Phases 33–37):** experiments, docs and publishing. Items marked *(solution branch)* wait for it.

---

# Part 1 — `master`: Reproduce the Failure

## Phase 1 — Repository Bootstrap

- [x] Create Java 21+ Maven repository.
- [x] Add Payout Platform application.
- [x] Add Payout Worker application.
- [x] Add Fake Bank Service application.
- [x] Add root Docker Compose.
- [x] Verify all applications compile.

## Phase 2 — Infrastructure

- [x] Add PostgreSQL for Payout Platform.
- [x] Add PostgreSQL for Fake Bank.
- [x] Add Redis.
- [x] Add Kafka.
- [x] Verify Docker Compose networking.
- [x] Verify both DBs start.
- [x] Verify Redis starts.
- [x] Verify Kafka starts.

## Phase 3 — Flyway / Hibernate

- [x] Add Flyway to Payout Platform.
- [x] Add Flyway to Fake Bank.
- [x] Set Hibernate to validate schema.
- [x] Disable Hibernate schema auto-creation as schema owner.
- [x] Verify migrations succeed.

## Phase 4 — Hexagonal Structure

- [x] Create domain layer.
- [x] Create application layer.
- [x] Create infrastructure adapters.
- [x] Create web adapters.
- [x] Keep Spring out of domain logic.
- [x] Keep Redis/Kafka/HTTP/JPA concerns outside domain.

Layout per app: `<context>/{domain/{aggregate,valueobject,event,service,exception}, application/{port/in,port/out,service}, adapter/in/api/{controller,dto,mapper,error}, adapter/out/<tech>, config}`.
Contexts: `platform.payout`, `fakebank.bank`; the worker's context package arrives with its first class.
Infrastructure is `adapter/out/<tech>` (persistence, kafka, redis, http) and the web adapter is `adapter/in/api`.
Each package is created with its first real class (Phase 5 onward), not as an empty placeholder.
The isolation items are enforced by each app's `ArchitectureTest` (ArchUnit): domain bans Spring, JPA/Hibernate/Flyway, Redis, Kafka and HTTP;
application reaches infrastructure only through ports. Each rule was checked against deliberately violating classes.
DDD building blocks are enforced too: value objects have only final fields and never depend on aggregates; aggregates have no public setters.

## Phase 5 — Payout Domain

- [x] Create Payout.
- [x] Add merchant ID.
- [x] Add amount.
- [x] Add currency.
- [x] Add READY status.
- [x] Add DISPATCHING status.
- [x] Add DISPATCHED status.
- [x] Add FAILED status if needed.
- [x] Add domain tests.

`platform.payout.domain.aggregate`: `Payout`. `platform.payout.domain.valueobject`: typed `PayoutId` / `MerchantId`,
`PayoutStatus`, and `Money` (amount + currency, held at the currency's minor-unit scale). Transitions: `prepare` → READY, `startDispatch` READY → DISPATCHING,
`markDispatched` DISPATCHING → DISPATCHED, `markFailed` DISPATCHING → FAILED; anything else throws `IllegalPayoutTransitionException`.
FAILED is kept for the bank failure modes (Phase 34).

## Phase 6 — Payout Platform Schema

- [x] Create payouts table.
- [x] Create outbox_events table.
- [x] Create payout_dispatch_attempts table.
- [x] Add indexes.
- [x] Add required migrations.
- [x] Verify schema in PostgreSQL.

Migrations `V2__create_payouts`, `V3__create_outbox_events`, `V4__create_payout_dispatch_attempts`.
Statuses and positive amount are CHECK constraints; an outbox row is PUBLISHED exactly when `published_at` is set;
attempts reference `payouts`. No fencing token, strategy or REJECTED_STALE status on `master`; the solution branch adds them (Phase 26). Indexes: pending outbox rows (partial, for the publisher poll), outbox by aggregate,
attempts by payout, payouts by merchant. `PayoutSchemaTest` covers constraints and indexes on a fresh Postgres;
the compose DB was migrated to v4 and inspected with `\d`.

## Phase 7 — Deterministic Data

- [x] Seed Merchant M-1001.
- [x] Seed Payout PO-9001.
- [x] Set amount = 252,000 EGP.
- [x] Set status = READY.
- [x] Ensure reset can recreate the exact same state.

The scenario values live once in `ScenarioPayout` (application layer), not in a SQL seed, so seed, reset and
prepare (Phase 22D) cannot drift apart. M-1001 is a merchant ID on the payout; merchants belong to another context,
so there is no merchants table. `SeedScenarioPayoutUseCase` runs at startup and only inserts PO-9001 if missing,
so a restart mid-scenario changes nothing. `ResetScenarioPayoutUseCase` overwrites it to READY / 252,000 EGP from any state;
Phase 24 builds the full reset on it. Covered by `ScenarioPayoutServiceTest` and `ScenarioPayoutPersistenceTest`.

## Phase 8 — Transactional Outbox

- [x] Persist payout.
- [x] Persist PayoutReady event in same transaction.
- [x] Verify atomic commit.
- [x] Add outbox status.
- [x] Add published timestamp.
- [x] Add integration test.

`PreparePayoutUseCase` resets PO-9001 to READY and appends a `PayoutReady` domain event through the `OutboxRepository` port,
in one `@Transactional` use case; nothing talks to Kafka here. The JPA adapter stores the event as a PENDING `outbox_events` row
(`published_at` null) with a plain-field JSON payload: eventId, payoutId, merchantId, amount, currency, occurredAt — the
contract the worker reads in Phase 10. Every prepare is a new event, so preparing twice gives two PENDING rows.
`TransactionalOutboxTest` checks the committed rows; `TransactionalOutboxRollbackTest` fails the outbox write after the
payout was saved and checks the payout change rolled back (it was confirmed to fail with `@Transactional` removed).

## Phase 9 — Outbox Publisher

- [x] Define OutboxPublisherPort.
- [x] Implement Kafka publisher adapter.
- [x] Poll unpublished rows.
- [x] Publish to `payout-ready`.
- [x] Mark published event.
- [x] Verify Kafka receives message.

`PublishOutboxUseCase` reads up to `batch-size` PENDING rows oldest first and handles one at a time: publish through
`OutboxPublisherPort`, wait for the broker's ack, then mark it PUBLISHED with `published_at`. The use case is not
`@Transactional` on purpose. If a send fails, the rows already published keep their marks, and the failed row and the
rows after it stay PENDING for the next poll, in order. Delivery is at-least-once: a crash between the ack and the
mark publishes the event again, which is why the worker cannot treat Kafka delivery as exactly-once. The mark only
changes rows that are still PENDING. `KafkaOutboxPublisherAdapter` sends the stored JSON payload unchanged, keyed by
payout ID. `payout-ready` is created at startup with one partition. `OutboxPublishScheduler` (`adapter/in/scheduler`)
polls with a fixed delay (`payout.outbox.publisher.poll-interval`, 500ms). `payout.outbox.publisher.enabled` turns it
off, and `src/test/resources/config/application.yml` does that for every test, so the PENDING-row tests don't race
the poller. `PublishOutboxServiceTest` covers ordering, batching and failure handling. `OutboxPublisherTest` turns
the poller back on, prepares PO-9001, reads the event from `payout-ready` on a Kafka container (key PO-9001, payload
fields intact), and waits for the row to become PUBLISHED.
The ArchitectureTest `..config..` patterns now start with `com.casestudies.payoutdelivery`, because the bare pattern
also matched `org.springframework.kafka.config`.

## Phase 10 — Kafka Consumer

- [x] Configure consumer group.
- [x] Consume PayoutReady.
- [x] Include event ID.
- [x] Include payout ID.
- [x] Include merchant ID.
- [x] Include amount/currency.
- [x] Verify worker receives message.

The worker's context is `worker.dispatch`. It has its own `PayoutId`, `MerchantId` and `Money` value objects, with the
same rules as the platform's. Nothing is shared across apps: the Kafka message is the only contract between them.
`PayoutReadyListener` (`adapter/in/kafka`) reads `payout-ready` and maps the JSON to `PayoutReadyMessage`, then to a
`PayoutReadyCommand` (event ID, payout ID, merchant ID, money, occurredAt) for `HandlePayoutReadyUseCase`. Unknown
fields are ignored, so the platform can add fields without breaking the worker. A malformed message is logged and
skipped instead of retried, because a retry can't fix it and would block the partition. All workers share the
consumer group `payout-workers`. `auto-offset-reset: earliest` means a new group still reads events published before
any worker started. `ack-mode: record` commits each offset only after `handle()` returns, so a crash mid-handle
redelivers the event (at-least-once). For now `HandlePayoutReadyService` only logs the receipt; Phases 16–18 add the
lease, the validation and the bank call. `PayoutReadyConsumerTest` sends the platform's JSONB payload format to a
Kafka container and checks every field of the command the use case receives. It also checks that malformed events
are skipped and the next valid one is still handled.

## Phase 11 — Fake Bank Application

- [x] Create separate Spring Boot service.
- [x] Add REST controller.
- [x] Add JPA.
- [x] Add Flyway.
- [x] Keep service intentionally small.

Phases 11–13 were built as one slice, because a controller is only useful once it has a table and an endpoint.
The app, its own Postgres, JPA (`validate`) and Flyway came from Phases 1–3. Context `fakebank.bank` has one aggregate,
`BankPayout`, and one use case, `CreateBankPayoutUseCase`. Its `PayoutId`, `MerchantId` and `Money` are its own
copies, the same as in the worker. `BankPayoutController` is the only controller.

## Phase 12 — Fake Bank Schema

- [x] Create bank_payouts table.
- [x] Add payout_id.
- [x] Add merchant_id.
- [x] Add amount.
- [x] Add currency.
- [x] Add worker_id.
- [x] Add received_at.
- [x] Add status.

Migration `V2__create_bank_payouts`. The `id` is a bank-assigned UUID. `payout_id` has no unique constraint, so a
duplicate request leaves a duplicate row. Amount must be positive. `status` allows only CREATED on `master`;
the solution branch adds IDEMPOTENT_REPLAY (Phase 32B). `bank_payouts_payout_id_idx (payout_id, received_at)` is
there for the duplicate and summary queries in Phase 21. `BankSchemaTest` covers duplicates, constraints and the index.

## Phase 13 — Fake Bank API

- [x] Add `POST /api/bank/payouts`.
- [x] Persist every request.
- [x] Return payout ID.
- [x] Verify duplicate requests create duplicate rows.

Body `{payoutId, merchantId, amount, currency}`; the caller identifies itself with the required `X-Worker-Id` header,
because the worker is diagnostic metadata, not part of the payout. Every valid request becomes a new CREATED row
(no lookup by payout ID); the response is `201` with `bankPayoutId` and the stored fields. A missing header, an
unreadable or invalid body, or an amount the currency can't hold is a `400` and persists nothing. `BankPayoutApiTest`
sends PO-9001 twice (as worker-b then worker-a) and finds two rows with different bank payout IDs, 504,000 EGP in total.

## Phase 14 — Fake Bank Behavior Modes

- [x] Add NORMAL.
- [x] Add DELAY_RESPONSE.
- [x] Add PROCESS_THEN_DELAY_RESPONSE.
- [x] Add RETURN_500.
- [x] Add TIMEOUT.
- [x] Make delay configurable.

`BankBehavior` (mode + delay) is a domain value object. It lives in `InMemoryBankBehaviorStore` (`adapter/out/memory`)
behind the `BankBehaviorStore` port, so it can be changed at runtime without a restart, and Phase 24 can reset it.
The startup value comes from `fake-bank.behavior.mode` / `fake-bank.behavior.delay` (NORMAL, 5s). `GET /api/bank/behavior`
returns `{mode, delayMillis}`; `PUT` takes the same body. `mode` is required. `delayMillis` is optional (0–120,000);
when it is missing, the current delay is kept. `CreateBankPayoutService` reads the mode on each request:
NORMAL persists and responds; DELAY_RESPONSE waits, then persists and responds (a late success);
PROCESS_THEN_DELAY_RESPONSE persists, then waits before it responds (the unknown outcome: a caller that times out
first was still paid); RETURN_500 persists nothing and returns `500`; TIMEOUT holds the request for the delay, then
returns `504` and persists nothing. `received_at` is when the request arrived, even when the row is written later.
The use case is no longer `@Transactional`: its single save commits on its own, so the PROCESS_THEN_DELAY_RESPONSE
row is visible before the delay starts and not only after the response. Waiting goes through a `Sleeper` port, so
`CreateBankPayoutServiceTest` checks the order of save and wait without real sleeps. `BankBehaviorApiTest` runs each
mode over HTTP. For PROCESS_THEN_DELAY_RESPONSE it sees the committed row while the request is still pending.

## Phase 15 — Bank Adapter

- [x] Define BankPayoutPort.
- [x] Implement HTTP adapter.
- [x] Send payout ID.
- [x] Send merchant ID.
- [x] Send amount/currency.

`BankPayoutPort.send(BankPayoutRequest)` takes `PayoutId`, `MerchantId`, `Money` and a new `WorkerId` value object, and
returns a `BankPayoutReceipt` with the bank's `bankPayoutId`. `HttpBankPayoutAdapter` (`adapter/out/http`) posts
`{payoutId, merchantId, amount, currency}` to `/api/bank/payouts` with the worker in `X-Worker-Id`, through a `RestClient`
on the JDK HTTP client (`spring-boot-starter-restclient`). Failures become port exceptions, so no HTTP type reaches the
application layer: a `4xx` is `BankRejectedException`; a `5xx` (RETURN_500, TIMEOUT's `504`) or a bank that can't be
reached is `BankUnavailableException`; no answer within `fake-bank.read-timeout` (10s) is `BankTimeoutException`, whose
outcome is unknown because the bank may already have paid (PROCESS_THEN_DELAY_RESPONSE). A connect timeout
(`fake-bank.connect-timeout`, 2s) counts as unavailable, since the request never left. The adapter sends once and never
retries; a retry would be a second source of duplicates on `master`. Nothing calls the port yet; Phase 18 does.
`HttpBankPayoutAdapterTest` runs the adapter against a JDK `HttpServer` stub: path, header, the four body fields and
the receipt, then `400`, `500`, `504`, a real read timeout and a refused connection. It was also run once against the
compose Fake Bank, and the row arrived with every field and the worker ID.

## Phase 16 — Redis Lease

- [ ] Define DistributedLeasePort.
- [ ] Implement Redis adapter.
- [ ] Use `payout:lease:{payoutId}`.
- [ ] Set TTL to 10 seconds for demo.
- [ ] Use unique owner ID.
- [ ] Prevent one owner from deleting another owner's lock.
- [ ] Verify expiration.

## Phase 17 — Dispatch Attempt History

- [ ] Persist worker ID.
- [ ] Persist payout ID.
- [ ] Persist lease owner.
- [ ] Persist started_at.
- [ ] Persist lease_expires_at.
- [ ] Persist validation_completed_at.
- [ ] Persist bank_request_sent_at.
- [ ] Persist completed_at.
- [ ] Persist final status.

## Phase 18 — Broken Worker Strategy

- [ ] Worker acquires lease.
- [ ] Worker loads payout.
- [ ] Worker validates payout.
- [ ] Worker performs final ownership/state check.
- [ ] Worker prepares HTTP request.
- [ ] Worker can pause after final check.
- [ ] Worker resumes from exact pause point.
- [ ] Worker sends HTTP request without re-running validation.

## Phase 19 — Broken Scenario

- [ ] Start Worker A.
- [ ] Worker A acquires lease.
- [ ] Worker A passes validation.
- [ ] Worker A pauses after validation.
- [ ] Keep A paused past lease TTL.
- [ ] Confirm lease expires.
- [ ] Start Worker B.
- [ ] Worker B acquires lease.
- [ ] Worker B calls Fake Bank.
- [ ] Fake Bank persists first payout.
- [ ] Worker B completes.
- [ ] Resume Worker A.
- [ ] Worker A sends stale prepared request.
- [ ] Fake Bank persists second payout.

## Phase 20 — Verify Broken Result

- [ ] Fake Bank DB contains 2 rows for PO-9001.
- [ ] Both rows contain 252,000 EGP.
- [ ] One came from Worker B.
- [ ] One came from Worker A.
- [ ] Total actual amount = 504,000 EGP.
- [ ] Duplicate exists in persisted bank data.

## Phase 21 — Query APIs

Payout Platform:
- [ ] Get payout.
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
- [ ] Explain prepare payout step.
- [ ] Explain outbox commit.
- [ ] Explain Kafka publication.
- [ ] Explain worker competition.
- [ ] Explain Worker A pause.
- [ ] Explain lease expiration.
- [ ] Explain Worker B takeover.
- [ ] Explain stale Worker A resume.
- [ ] Explain expected result: a duplicate payout at the bank.

## Phase 22C — Scenario Controls

- [ ] Add worker-count selector.
- [ ] Support 2 workers.
- [ ] Support 3 workers.
- [ ] Support 4 workers.
- [ ] Support 5 workers.
- [ ] Default worker count to 2.
- [ ] Keep Worker A as deterministic delayed worker.

## Phase 22D — Prepare Payout Action

- [ ] Add `Prepare Payout` button.
- [ ] Call `POST /api/payouts/prepare`.
- [ ] Create/update PO-9001.
- [ ] Create PayoutReady outbox event in same DB transaction.
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
- [ ] Send worker count.
- [ ] Use deterministic PO-9001.
- [ ] Keep same amount and merchant.
- [ ] Keep the same timing on every run.
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
- [ ] Show payout ID.
- [ ] Show worker ID.
- [ ] Show amount.
- [ ] Show received timestamp.
- [ ] Show processing result.
- [ ] Group by business payout.
- [ ] Detect duplicates from persisted data.
- [ ] Highlight duplicate rows.
- [ ] Add DUPLICATE badge.

## Phase 22I — Result Summary

- [ ] Show FAILURE DETECTED.
- [ ] Show selected worker count.
- [ ] Show bank requests attempted.
- [ ] Show actual financial payouts.
- [ ] Show duplicate payout count.
- [ ] Show intended amount.
- [ ] Show actual bank amount.

## Phase 22J — Clear & Reset

- [ ] Add `Clear & Reset` button.
- [ ] Call scenario reset endpoint.
- [ ] Delete payout dispatch attempts.
- [ ] Delete/reset outbox scenario data.
- [ ] Reset payout state.
- [ ] Delete Fake Bank payout rows.
- [ ] Delete Fake Bank request-attempt history if present.
- [ ] Clear Redis lease keys.
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

- [ ] Group bank payouts by payout_id.
- [ ] Highlight count > 1.
- [ ] Add DUPLICATE badge.
- [ ] Show Bank Requests Received.
- [ ] Show Unique Payouts.
- [ ] Show Duplicate Bank Payouts.
- [ ] Show Total Intended Amount.
- [ ] Show Total Actual Amount.
- [ ] Show FAILURE DETECTED.

## Phase 24 — Reset Scenario

Payout Platform:
- [ ] Delete dispatch attempts.
- [ ] Reset PO-9001 to READY.
- [ ] Reset/recreate outbox state.
- [ ] Clear Redis lease.

Fake Bank:
- [ ] Delete bank payout rows.
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

# Part 2 — Solution Branch: Fix the Failure

Branch from `master` once Phase 25 is done. Never edit `master`'s migrations; add new ones.

## Phase 26 — Add Fencing Tokens

- [ ] Design monotonic fencing token generation.
- [ ] Add token to lease acquisition.
- [ ] Worker A gets older token.
- [ ] Worker B gets newer token.
- [ ] Persist token in dispatch attempts.
- [ ] Add `V5` migration: nullable `fencing_token`, `strategy` (BROKEN / PROTECTED), REJECTED_STALE attempt status.
- [ ] Persist strategy.
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

- [ ] Define `payout:PO-9001`.
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
- [ ] Run PO-9001.
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

## Phase 32B — Solution Additions to Part 1 Phases

Fake Bank (Phases 12, 13, 15, 24):
- [ ] Add `idempotency_key` to `bank_payouts` (new migration).
- [ ] Send `Idempotency-Key` from the bank adapter.
- [ ] Clear idempotency state on reset.

Frontend (Phases 22B–22J):
- [ ] Add Scenario Mode selector with BROKEN and PROTECTED options.
- [ ] Send selected mode when running the scenario.
- [ ] Keep the same timing between broken and protected runs.
- [ ] Explain broken vs protected expected result; change explanation by mode.
- [ ] Show idempotency key in the bank operations table.
- [ ] Show IDEMPOTENT_REPLAY rows.
- [ ] Protected result summary: show PROTECTED, selected worker count, bank requests attempted,
      actual financial payouts = 1, idempotent replay count, duplicate payout count = 0, actual amount = intended amount.
- [ ] Clear bank idempotency records and fencing state on Clear & Reset.

---

# Part 3 — Both Branches

## Phase 33 — Kafka Reliability Experiments

Only after core scenario works:

- [ ] Redeliver same Kafka event.
- [ ] Restart consumer after processing.
- [ ] Simulate consumer rebalance.
- [ ] Verify bank idempotency still protects payout. *(solution branch)*

## Phase 34 — Fake Bank Failure Experiments

Only after core scenario works:

- [ ] DELAY_RESPONSE.
- [ ] PROCESS_THEN_DELAY_RESPONSE.
- [ ] RETURN_500.
- [ ] TIMEOUT.
- [ ] Verify retries do not duplicate payout when idempotency is enabled. *(solution branch)*

## Phase 35 — Documentation

- [ ] Explain Transactional Outbox.
- [ ] Explain Kafka at-least-once behavior.
- [ ] Explain lease expiration.
- [ ] Explain stale worker.
- [ ] Explain check-then-act gap.
- [ ] Explain fencing. *(solution branch)*
- [ ] Explain fencing limitation for external APIs. *(solution branch)*
- [ ] Explain stable business idempotency. *(solution branch)*
- [ ] Add broken sequence diagram.
- [ ] Add protected sequence diagram. *(solution branch)*

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
- [ ] Protected scenario uses identical data/timing. *(solution branch)*
- [ ] Protected scenario creates one financial payout. *(solution branch)*
- [ ] Replay is visible. *(solution branch)*
- [ ] Outbox role is documented correctly.
- [ ] Kafka role is documented correctly.
- [ ] Fencing limitation is documented correctly. *(solution branch)*
- [ ] Idempotency role is documented correctly. *(solution branch)*

Final takeaway:

```text
Outbox prevents lost publication.
Kafka may redeliver.
Leases can expire while workers are alive.
Fencing protects cooperating internal resources.
External financial side effects need business-level idempotency.
```
