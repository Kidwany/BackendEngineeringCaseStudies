# Payout Delivery Reliability Case Study

## Title
**When At-Least-Once Delivery Meets an Expired Lease: Preventing Duplicate Payouts**

Repository name suggestion:

`payout-delivery-failure-lab`

---

## Goal

Build a realistic payment-oriented distributed systems case study showing how a duplicate payout can happen even when the system already uses:

- Transactional Outbox
- Kafka
- Redis-based distributed lease/lock
- Multiple payout workers

Then rerun the exact same scenario with protection mechanisms enabled and prove from persisted data that only one financial payout is created.

The case study must show the broken behavior first, then the protected behavior, using the same payout data and timing.

---

## Technology Stack

- Java 21+
- Spring Boot
- Maven
- Spring Data JPA
- Hibernate
- PostgreSQL
- Redis
- Kafka
- Flyway
- Docker Compose
- JUnit 5

Use REST/HTTP between the payout worker and the Fake Bank Service.

Use Kafka for asynchronous payout-ready delivery.

Use Redis for distributed lease coordination.

Use PostgreSQL for both the payout system and Fake Bank Service.

Use Flyway as the schema owner.

Recommended:

`spring.jpa.hibernate.ddl-auto=validate`

---

## Architecture

Use:

- Domain-Driven Design
- Hexagonal Architecture
- Ports and Adapters
- Dependency inversion

Keep the project intentionally small.

Recommended applications/components:

1. Payout Platform
2. Payout Worker
3. Fake Bank Service

The Outbox Publisher may live inside the Payout Platform.

High-level flow:

```text
Payout Platform
    |
    | DB transaction
    | - create/update payout batch
    | - create outbox event
    v
Outbox Publisher
    |
    v
Kafka topic: payout-ready
    |
    v
Payout Worker
    |
    | Redis lease / fencing
    v
Bank Adapter
    |
    | HTTP
    v
Fake Bank Service
    |
    v
Fake Bank PostgreSQL DB
```

---

## Business Scenario

The platform prepares a merchant payout batch.

Example:

```text
Merchant: M-1001
Payout Batch: PB-9001
Amount: 252,000 EGP
```

Inside one local DB transaction:

1. Create/update payout batch.
2. Mark it READY.
3. Insert outbox event `PayoutBatchReady`.
4. Commit.

The Outbox Publisher later publishes the event to Kafka.

---

## Payout Batch

Suggested fields:

```text
id
merchant_id
amount
currency
status
created_at
updated_at
```

Suggested statuses:

```text
READY
DISPATCHING
DISPATCHED
FAILED
```

---

## Outbox Event

Suggested fields:

```text
id
aggregate_id
event_type
payload
status
created_at
published_at
```

Example:

```text
Event ID: EVT-5512
Type: PayoutBatchReady
Batch ID: PB-9001
Amount: 252000
Currency: EGP
```

Kafka topic:

`payout-ready`

---

## Payout Worker

Support two logical workers:

```text
Worker A
Worker B
```

Each execution should track:

```text
workerId
payoutBatchId
leaseOwnerId
fencingToken
startedAt
leaseExpiresAt
validationCompletedAt
bankRequestSentAt
completedAt
status
```

---

## Redis Lease

Use a Redis-based lease behind a port.

Conceptual key:

`payout:batch:{batchId}`

Demo TTL:

`10 seconds`

Do not couple application/domain logic directly to Redis.

---

# Broken Scenario

The broken scenario must intentionally create a duplicate payout in the Fake Bank database.

## Initial State

```text
Payout Batch: PB-9001
Merchant: M-1001
Amount: 252,000 EGP
Status: READY
```

A `PayoutBatchReady` outbox event is published to Kafka.

## Worker A

Worker A:

1. Acquires Redis lease.
2. Loads payout.
3. Validates payout state.
4. Performs its final ownership/state check successfully.
5. Prepares the Fake Bank HTTP request.
6. Pauses **after the final successful check but before the HTTP call**.

Example timeline:

```text
T=0  Worker A acquires lease
T=1  Worker A validates payout
T=2  Worker A validates ownership
T=3  Worker A prepares bank request
T=4  Worker A pauses
```

Worker A remains alive and resumes from the next instruction after the pause.

## Lease Expiration

```text
T=10 Worker A lease expires
```

## Worker B

After expiry:

```text
T=11 Worker B acquires the same payout lease
```

Worker B:

1. Validates payout.
2. Builds bank request.
3. Calls Fake Bank Service.
4. Fake Bank persists the payout.
5. Worker B completes.

## Worker A Resumes

Worker A resumes from the point after its previous validation.

It does not restart the method and does not re-run the database check.

It sends its already prepared request to the Fake Bank.

Without external idempotency, the Fake Bank stores a second payout row.

Expected broken result:

```text
Expected financial payouts: 1
Actual financial payouts:   2
Duplicate payout: YES
```

---

## Important Failure Principle

The failure is not:

> We forgot to check the database again.

The failure is:

```text
check passed
worker paused
ownership changed
worker resumed after check
external side effect executed
```

A second pre-action check is not a correctness guarantee because the same pause can happen immediately after that check.

---

# Fake Bank Service

Create a separate Spring Boot application.

Expose:

`POST /api/bank/payouts`

Example request:

```json
{
  "payoutBatchId": "PB-9001",
  "merchantId": "M-1001",
  "amount": 252000,
  "currency": "EGP"
}
```

The Fake Bank must use its own PostgreSQL database.

Suggested table:

```text
bank_payouts
```

Suggested fields:

```text
id
payout_batch_id
merchant_id
amount
currency
idempotency_key
worker_id
received_at
processing_status
```

In the broken version:

- allow duplicate payout rows
- do not enforce idempotency yet

The database must preserve both duplicate payouts.

---

## Fake Bank Behavior Modes

Support configurable modes:

```text
NORMAL
DELAY_RESPONSE
PROCESS_THEN_DELAY_RESPONSE
RETURN_500
TIMEOUT
```

For the first stale-worker experiment, use `NORMAL`.

Later, these modes can test retries and unknown outcomes.

---

# Internal Dispatch Attempts

Persist internal worker execution history.

Suggested table:

```text
payout_dispatch_attempts
```

Suggested fields:

```text
id
payout_batch_id
worker_id
strategy
lease_owner_id
fencing_token
status
started_at
lease_expires_at
validation_completed_at
bank_request_sent_at
completed_at
failure_reason
```

Suggested statuses:

```text
STARTED
VALIDATED
PAUSED
SENT_TO_BANK
SUCCESS
REJECTED_STALE
FAILED
```

---

# Web Interface

Create a simple UI with:

```text
[ Run Broken Scenario ]
[ Run Protected Scenario ]
[ Reset Scenario ]
```

Recommended sections:

1. Scenario Controls
2. Payout Batch Details
3. Outbox Event
4. Worker Timeline
5. Redis Lease / Fencing Information
6. Fake Bank Operations
7. Duplicate Detection
8. Result Summary

---

# Fake Bank Operations View

The UI must fetch actual Fake Bank operations from the Fake Bank API.

Example broken result:

```text
Bank Payout #1
Batch: PB-9001
Amount: 252,000 EGP
Worker: B

Bank Payout #2
Batch: PB-9001
Amount: 252,000 EGP
Worker: A
DUPLICATE
```

Duplicate rows should be visually highlighted.

Duplicate detection must come from persisted data, for example by grouping on `payout_batch_id`.

Show summary:

```text
Bank Requests Received: 2
Unique Payout Batches: 1
Duplicate Bank Payouts: 1
Total Intended Amount: 252,000 EGP
Total Actual Payout Amount: 504,000 EGP
```

Display:

`FAILURE DETECTED`

when duplicates exist.

---

# Reset Scenario

Reset both systems without dropping databases.

Payout Platform reset:

- delete dispatch attempts
- reset PB-9001 to READY
- reset/recreate outbox state
- clear Redis lease
- reset fencing state when added

Fake Bank reset:

- delete bank payout rows
- clear idempotency state
- reset behavior mode to NORMAL

The same deterministic data must be recreated.

---

# Protected Scenario

Use exactly the same:

```text
PB-9001
M-1001
252,000 EGP
Worker A
Worker B
lease TTL
pause timing
```

Do not make the protected scenario easier.

---

# Fencing Token Phase

When Worker A acquires ownership:

```text
Fence Token = 41
```

After expiry, Worker B:

```text
Fence Token = 42
```

Propagate fencing tokens through the internal payout execution flow.

However, explicitly demonstrate this limitation:

> A fencing check performed before an external HTTP call cannot, by itself, prevent Worker A from resuming after the check and calling the external bank.

This limitation must be documented and visible in the code path.

---

# External Idempotency

Use a stable business idempotency key for the external bank operation.

Example:

`payout:PB-9001`

The key must be based on the payout business operation.

Do not use:

- worker ID
- Kafka event ID
- retry attempt ID
- fencing token

Both Worker A and Worker B must generate the same idempotency key for PB-9001.

---

# Fake Bank Idempotency

Protected-mode behavior:

First request:

```text
Idempotency-Key: payout:PB-9001
```

Result:

```text
Payout created
```

Second request with the same key:

```text
Idempotency-Key: payout:PB-9001
```

Result:

```text
No second financial payout
Return original result
Mark request as IDEMPOTENT_REPLAY
```

The Fake Bank should contain one actual financial payout.

Optionally keep a separate bank request-attempt history so the UI can show:

```text
HTTP attempts: 2
Actual payouts: 1
Idempotent replays: 1
```

---

# Protected Scenario Expected Result

Expected:

```text
Bank Calls Attempted: 2
Actual Financial Payouts Created: 1
Duplicate Payouts: 0
```

UI:

```text
PB-9001   252,000 EGP   CREATED
PB-9001                  IDEMPOTENT_REPLAY
```

---

# Before / After Comparison

## Broken

```text
Strategy:
Outbox + Kafka + Redis Lease

Bank Calls Attempted: 2
Actual Bank Payout Rows: 2
Duplicate Payouts: 1
Total Actual Amount: 504,000 EGP
```

## Protected

```text
Strategy:
Outbox + Kafka + Lease/Fencing + Bank Idempotency

Bank Calls Attempted: 2
Actual Bank Payout Rows: 1
Duplicate Payouts: 0
Idempotent Replays: 1
Total Actual Amount: 252,000 EGP
```

---

# Communication Protocols

Payout Platform → Kafka:

`Event-driven`

Kafka → Payout Worker:

`Kafka consumer`

Payout Worker → Fake Bank:

`HTTP REST`

Do not add gRPC unless a later case study specifically needs it.

---

# Hexagonal Ports

Useful ports may include:

```text
OutboxPublisherPort
DistributedLeasePort
PayoutRepository
PayoutDispatchAttemptRepository
BankPayoutPort
```

Possible adapters:

```text
KafkaOutboxPublisherAdapter
RedisDistributedLeaseAdapter
JpaPayoutRepositoryAdapter
HttpFakeBankAdapter
```

Keep Kafka, Redis, HTTP, Spring Data, and JPA details outside the domain model.

---

# Lightweight Command / Query Separation

Commands:

```text
CreatePayoutBatch
RunBrokenScenario
RunProtectedScenario
ResetScenario
```

Queries:

```text
GetScenarioResult
GetDispatchAttempts
GetOutboxEvents
GetBankOperations
GetDuplicateBankPayouts
```

Do not introduce a separate read database.

---

---

# Frontend / Interactive Case Study Flow

The frontend is an essential part of the case study.

Its purpose is not to be visually complex.

Its purpose is to make the distributed-systems behavior understandable to someone opening the repository without reading the source code first.

Use a simple frontend technology.

Preferred:

```text
React
```

Angular or another lightweight frontend is acceptable if implementation is simpler.

Do not spend unnecessary time on styling.

Focus on:

- clarity
- scenario controls
- persisted data visualization
- execution timeline
- before/after comparison

---

## Main Page Title

At the top of the page, display a clear case-study title such as:

```text
Payout Delivery Failure Lab
```

Subtitle:

```text
How Outbox, Kafka, worker leases, stale workers, fencing, and idempotency interact in a real payout pipeline.
```

---

## Scenario Explanation Panel

Before the user starts the scenario, show a short explanation of what will happen.

Example:

```text
What will happen when you start this scenario?

1. A merchant payout batch will be created.
2. The batch and PayoutBatchReady outbox event will be committed atomically.
3. The Outbox Publisher will publish the event to Kafka.
4. Multiple payout workers will compete to process the payout.
5. Worker A will intentionally pause after its final ownership check.
6. Its lease will expire.
7. Another worker will acquire the payout and send it to the Fake Bank.
8. Worker A will later resume and attempt the same bank payout.
9. In the broken scenario, the Fake Bank will create a duplicate payout.
10. In the protected scenario, stable business idempotency will prevent the duplicate financial operation.
```

The explanation should change slightly depending on the selected strategy.

---

# Step 1 — Prepare Payout Batch

The frontend should NOT directly publish an outbox event.

Instead expose a business action:

```text
[ Prepare Payout Batch ]
```

When clicked:

```text
Frontend
    ↓
POST /api/payout-batches/prepare
    ↓
Payout Application Service
    ↓
Local DB Transaction
    ├── create/update PB-9001
    └── insert PayoutBatchReady outbox event
    ↓
COMMIT
```

After this action, the UI should display persisted state such as:

```text
Payout Batch:
PB-9001
READY
252,000 EGP

Outbox Event:
EVT-5512
PayoutBatchReady
PENDING
```

The Outbox Publisher should publish the event independently.

The frontend does not directly call Kafka.

---

# Step 2 — Worker Count Selector

Provide a simple worker count selector.

Example:

```text
Number of Workers

[ 2 ▼ ]
```

Suggested allowed values:

```text
2
3
4
5
```

Default:

```text
2
```

The number should control how many logical payout workers participate in the simulation.

For the stale-worker demonstration, Worker A should always be the intentionally delayed worker.

Other workers may compete normally.

The scenario runner should make the behavior deterministic enough that the intended failure can still be reproduced even when more than two workers are selected.

---

# Step 3 — Scenario Strategy Selector

Provide:

```text
Scenario Mode

[ Broken Scenario ▼ ]
```

Options:

```text
Broken Scenario
Protected Scenario
```

The selected mode determines the worker strategy and Fake Bank behavior.

## Broken Scenario

Uses:

```text
Transactional Outbox
Kafka
Redis Lease
No external business idempotency
```

Expected result:

```text
Duplicate payout possible
```

## Protected Scenario

Uses:

```text
Transactional Outbox
Kafka
Redis Lease
Fencing-aware internal strategy
Stable business idempotency at Fake Bank boundary
```

Expected result:

```text
One financial payout
Second request treated as idempotent replay
```

---

# Step 4 — Run Scenario

Provide:

```text
[ Run Scenario ]
```

This button should be enabled only when a payout batch has been prepared.

When clicked, the UI should execute the scenario using:

```text
selected worker count
selected scenario mode
same deterministic payout batch
same configured timing
```

Do not generate random payout amounts or random business identities.

The same scenario inputs should be reused so results are directly comparable.

---

# Step 5 — Live / Progressive Visualization

While the scenario runs, the UI should progressively show important state changes.

Example:

```text
Payout Batch
PB-9001
READY

Outbox
PENDING
↓
PUBLISHED

Kafka
PayoutBatchReady received

Workers
Worker A — LEASE ACQUIRED
Worker A — VALIDATED
Worker A — PAUSED

Worker B — WAITING
Worker B — LEASE ACQUIRED
Worker B — BANK REQUEST SENT

Worker A — RESUMED
Worker A — BANK REQUEST SENT
```

The UI does not need real-time WebSockets initially.

Simple polling is acceptable.

For example:

```text
GET /api/scenarios/current
```

every 500–1000ms while the scenario is running.

Do not introduce WebSockets unless there is a clear need.

---

# Step 6 — Fake Bank Operations View

Display persisted bank operations from the Fake Bank Service.

Example broken result:

```text
Bank Operations

#1
PB-9001
252,000 EGP
Worker B
CREATED

#2
PB-9001
252,000 EGP
Worker A
CREATED
DUPLICATE
```

Repeated payout batch rows must be highlighted visually.

Example styling ideas:

```text
red border
warning background
DUPLICATE badge
```

Do not hard-code which row is duplicated.

Determine duplicates from persisted Fake Bank data.

---

# Step 7 — Result Summary

Broken result example:

```text
RESULT: FAILURE DETECTED

Workers: 2
Bank Requests Attempted: 2
Actual Financial Payouts: 2
Duplicate Payouts: 1

Intended Amount:
252,000 EGP

Actual Amount Created At Bank:
504,000 EGP
```

Protected result example:

```text
RESULT: PROTECTED

Workers: 2
Bank Requests Attempted: 2
Actual Financial Payouts: 1
Idempotent Replays: 1
Duplicate Payouts: 0

Actual Amount Created At Bank:
252,000 EGP
```

---

# Reset / Truncate Scenario

Provide a clear button:

```text
[ Clear & Reset ]
```

The button should return the entire experiment to the deterministic initial state.

The user described this as "truncate", but implementation should prefer safe explicit cleanup rather than dropping schemas.

The reset flow should clean:

## Payout Platform Database

```text
payout_dispatch_attempts
outbox_events
scenario payout batch data
scenario execution history
```

Then recreate:

```text
PB-9001
M-1001
252,000 EGP
```

in the correct initial state when appropriate.

## Fake Bank Database

Clean:

```text
bank_payouts
bank_request_attempts if implemented
idempotency records
```

## Redis

Clean scenario-specific:

```text
leases
fencing sequence/state
```

## Kafka

Do not attempt to truncate the whole Kafka broker.

Instead design scenario messages so previous runs do not contaminate new runs.

Possible approaches:

```text
scenarioRunId
consumer correlation
scenario-specific cleanup semantics
```

The reset must guarantee that the next run starts from a clean logical scenario.

---

# Optional Full Reset Button

If useful, expose two buttons:

```text
[ Clear Results ]
[ Reset Entire Scenario ]
```

Where:

```text
Clear Results
```

clears execution and bank history but keeps the prepared batch.

And:

```text
Reset Entire Scenario
```

returns the application to the state before `Prepare Payout Batch`.

This is optional.

A single reliable `Clear & Reset` button is sufficient for the first implementation.

---

# Suggested Page Layout

```text
--------------------------------------------------
Payout Delivery Failure Lab
--------------------------------------------------

[ What will happen? explanation ]

Scenario Mode:
[ Broken Scenario ▼ ]

Workers:
[ 2 ▼ ]

[ Prepare Payout Batch ]

--------------------------------------------------
Prepared Business State

Batch: PB-9001
Merchant: M-1001
Amount: 252,000 EGP
Status: READY

Outbox:
EVT-5512
PayoutBatchReady
PENDING
--------------------------------------------------

[ Run Scenario ]

--------------------------------------------------
Execution Timeline

Outbox → Kafka → Worker A → Worker B → Bank
...
--------------------------------------------------

Fake Bank Operations

...
--------------------------------------------------

Result Summary

...
--------------------------------------------------

[ Clear & Reset ]
```

---

# Frontend API Requirements

Suggested APIs:

```text
POST /api/payout-batches/prepare

POST /api/scenarios/run
{
  "mode": "BROKEN",
  "workerCount": 2
}

POST /api/scenarios/reset

GET /api/scenarios/current
GET /api/payout-batches/PB-9001
GET /api/outbox-events
GET /api/dispatch-attempts
```

Fake Bank APIs:

```text
GET /api/bank/payouts
GET /api/bank/payouts/summary
GET /api/bank/payouts/duplicates
```

The Payout Platform frontend may call the Fake Bank API directly for the demo, or the Payout Platform may aggregate the data.

Prefer the simpler approach.

Do not create an API Gateway just for the case study.

---

# Frontend Engineering Rule

The frontend should explain the distributed system.

It is not a frontend portfolio project.

Avoid spending time on:

```text
complex component libraries
authentication
routing complexity
state-management frameworks
animations
design systems
```

A small React application with clear components and polling is enough.

Suggested components:

```text
ScenarioExplanation
ScenarioControls
PreparedPayoutPanel
OutboxPanel
WorkerTimeline
BankOperationsTable
ScenarioResultSummary
ResetButton
```

# Testing

Add useful tests for:

- payout domain behavior
- atomic outbox creation
- outbox publishing
- Kafka consumption
- Redis lease expiration
- Worker A stale resumption
- Fake Bank persistence
- duplicate detection
- bank idempotency
- reset scenario
- protected scenario

Use integration tests with real infrastructure where practical.

Testcontainers may be used.

---

# README Topics

Explain:

- Why Transactional Outbox prevents lost publication
- Why it does not guarantee exactly-once payout execution
- Why Kafka may redeliver
- Why Redis lease expiration can create a stale worker
- Why another database check is not a full correctness guarantee
- What fencing protects
- Why fencing alone cannot protect a non-cooperating external API
- Why stable business idempotency is needed at the bank boundary
- How all mechanisms complement each other

Final engineering takeaway:

```text
Outbox prevents lost publication.

Kafka provides durable asynchronous delivery but may redeliver.

Leases coordinate workers but can expire while a worker is still alive.

Fencing protects cooperating internal resources from stale ownership.

External financial side effects require business-level idempotency.
```
