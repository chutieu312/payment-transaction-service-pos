# Temu Interview Simulation — Full Q&A Reference
**Candidate:** Can Nguyen | **Project anchor:** PAT Financial Operations Service (JPMC)
**Date practiced:** 2026-08-20 / 2026-08-21

---

## HOW TO USE THIS DOCUMENT
- Read each question. Cover the answer. Try to recall it yourself first.
- Focus on the **bold key points** — those are the sentences that land.
- Every answer should start from PAT-FOS, then generalize.

---

# INTERVIEW 1 — Temu Java Engineer (Order System)

> **Role focus:** High-concurrency order system, high availability, distributed transactions, MySQL/Redis/MQ.

---

## Q1 · Tell me about yourself

**Anchor line:**
> "I'm Can Nguyen — a software engineer with five years of experience focused on Java backend and distributed systems."

**Hit these points in order:**
1. Current role: Technical Reviewer at Mercor/HandshakeAI — evaluating AI benchmark tasks, writing Docker-containerized test environments.
2. JPMC (3+ years): PAT Financial Operations Service — inter-team budget transfers, event-driven fraud detection.
3. Stack: Java 21, Spring Boot, Kafka, Redis, PostgreSQL, MongoDB, Kubernetes, AWS.
4. Why Temu: the order system problem is a harder version of the same financial transaction problem I've been solving.

---

## Q2 · You handled concurrent writes with pessimistic locking. Go deeper — problem, implementation, trade-offs.

**The problem:**
Two concurrent transfers from the same account both read the same balance, both think there's enough money, both proceed → overdraft.

**Implementation — `SELECT FOR UPDATE`:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.id = :id")
Optional<Account> findByIdForUpdate(@Param("id") UUID id);
```
Debit + credit happen inside a single `@Transactional` method. Lock released on commit.

**Trade-offs:**

| | Pessimistic Locking | Optimistic Locking |
|---|---|---|
| How | `SELECT FOR UPDATE` — blocks other transactions | Version column — retry on conflict |
| Best for | High contention, money | Low contention, reads |
| Cost | Serialization under load | Retry storms under high conflict |

**Why I chose pessimistic in PAT-FOS:** money is involved — correctness > throughput.

---

## Q3 · At 10K orders/second, pessimistic locking on inventory collapses. How do you redesign?

**Key insight: the database cannot be on the hot path for 100K writes/second.**

**Solution — Redis atomic DECR as the reservation layer:**
```
Before sale: SET inventory:{productId} 10000

Per order:
  result = DECR inventory:{productId}
  if result < 0:
    INCR inventory:{productId}   ← compensate
    return 429 SOLD_OUT
  else:
    publish to Kafka → return 202
```
Redis is single-threaded per command — `DECR` is atomic by design. No locks, no contention.

**Full flow:**
1. Redis atomic DECR → reservation confirmed
2. Publish to Kafka → return 202 immediately
3. Kafka consumer writes to MySQL asynchronously
4. MySQL idempotency key prevents duplicates

**Other options considered:**
- Optimistic locking with version column → retry storms at extreme scale
- Pre-sharding inventory (10 partition rows per product) → reduces contention 10x
- Reservation + async confirmation → what I use with Kafka

---

## Q4 · Your service has one DB. What if the transfer involves two microservices, each with their own DB? How do you ensure consistency without a distributed transaction?

**Answer: Choreography-based Saga Pattern**

> "You can't do two-phase commit across services — it's slow, tight coupling, single point of failure. Instead: sequence of local transactions, each publishing an event. Failures trigger compensating transactions."

**PAT-FOS saga:**
```
Step 1: Save PENDING_FRAUD_CHECK → commit locally → publish to Kafka
Step 2: Fraud service evaluates → publishes APPROVED/REJECTED
Step 3: If APPROVED → debit + credit in @Transactional → publish COMPLETED
        If REJECTED → publish FRAUD_REJECTED → no money moves
```

**Challenges I handled explicitly:**
- **Idempotency:** consumer checks `findByTransactionId()` before processing — safe to replay
- **At-least-once delivery:** Kafka guarantees this, so consumers must be idempotent
- **Failure visibility:** every status transition in MongoDB audit log — reconstruct state at any point

**Trade-off vs 2PC:** eventual consistency — there's a window where the saga is in-flight. Acceptable if compensation path is solid and auditable.

---

## Q5 · Your experience is PostgreSQL. What are meaningful differences vs MySQL for high-write systems?

**Be honest: production experience is PostgreSQL. Then show you know MySQL.**

| Topic | PostgreSQL | MySQL (InnoDB) |
|---|---|---|
| MVCC | Yes — readers never block writers | Yes — same |
| Gap locking | No gap locks (uses predicate locks for serializable) | Yes — can cause unexpected deadlocks on high-insert |
| Sharding ecosystem | Citus (good) | Vitess (more mature — YouTube, PlanetScale use it) |
| JSON | JSONB — indexed, queryable | JSON type — less powerful |
| Auto-increment | Sequences | `AUTO_INCREMENT` — single-node bottleneck when sharding → use Snowflake IDs instead |

**Key MySQL-specific things to learn:**
- `EXPLAIN` format differences
- `innodb_buffer_pool_size` tuning
- `INSERT ... ON DUPLICATE KEY UPDATE` vs `REPLACE INTO` for idempotent upserts

---

## Q6 · Coding: First Duplicate Order ID

```
Input:  [101, 203, 305, 101, 203]  → Output: 101
Input:  [5, 1, 3, 5, 3]           → Output: 5
Input:  [1, 2, 3]                  → Output: -1
```

**Approach:** single pass, HashSet. First time `add()` returns false = first duplicate.

```java
public int firstDuplicateOrder(int[] orderIds) {
    Set<Integer> seen = new HashSet<>();
    for (int orderId : orderIds) {
        if (!seen.add(orderId)) {   // add() returns false if already present
            return orderId;
        }
    }
    return -1;
}
```
**Time:** O(n) · **Space:** O(n)

**Follow-up — prevent duplicates in distributed system (DB level):**
```sql
CREATE TABLE orders (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,  ← database-level safety net
    ...
);
```
```java
try {
    orderRepository.save(order);
} catch (DataIntegrityViolationException e) {
    return orderRepository.findByIdempotencyKey(order.getIdempotencyKey());
}
```
> "Check-then-act pattern breaks under concurrency — two pods both check, both see 'not exists,' both insert. The UNIQUE constraint is the only correct solution."

---

## Q7 · System Design: High-concurrency order system, 100K orders/second at flash sale peak

**Start with requirements:**
- Functional: submit order → reserve inventory → confirm → notify
- Non-functional: p99 < 500ms authorization, no overselling, 99.99% availability

**Architecture:**
```
Client → API Gateway (rate limit, auth) → Order Service (stateless, N pods)
  ↓
[Synchronous — hot path]
  Redis DECR inventory → if success → save order PENDING → Kafka → return 202

[Async — Kafka consumers]
  Order workers → write to MySQL → Notification service
```

**Why Redis on hot path:**
- MySQL cannot sustain 100K writes/second on inventory rows
- Redis `DECR` is atomic, single-threaded — no contention
- DB write happens async via Kafka — no blocking

**MySQL sharding:**
- Shard by `user_id` hash — 16 shards
- Snowflake IDs for primary keys — no `AUTO_INCREMENT` bottleneck across shards
- Idempotency key UNIQUE constraint on each shard

**Failure scenarios:**
| Failure | Mitigation |
|---|---|
| Redis down | Circuit breaker → fallback to DB-backed reservation |
| Kafka consumer crash | Offset not committed → message replayed → idempotent consumer |
| Redis counter drift | Periodic reconciliation job — MySQL order count is source of truth |

**Redis vs MySQL — whose number to trust:**
> "MySQL is the source of truth for committed orders. Redis counter can have phantom decrements from failed Kafka publishes. I reset Redis from MySQL counts — but never set it higher than it currently reads, because in-flight reservations might not be persisted yet."

---

## Q8 · Behavioral: Production issue under time pressure

**Story: Kafka deserialization mismatch, 2 hours before demo**

- **Situation:** Transfers stuck in `PENDING_FRAUD_CHECK` — fraud service not consuming events
- **Diagnose:** Kafka UI → events published ✓ → fraud-service logs → deserialization error
- **Root cause:** Java serializes to `camelCase`, Python Pydantic expected `snake_case`
- **Fix:** Added field aliases + `model_config = ConfigDict(populate_by_name=True)` in Python
- **Result:** Fixed in 45 minutes, demo ran successfully
- **Prevention:** Added contract test — Java-format event → Python consumer must deserialize without error

---

## Q9 · Behavioral: Zero tolerance for production quality without becoming a bottleneck

**Key answer framework:**
1. **Protect the money path obsessively** — `@Valid`, UNIQUE constraints, `@Transactional` — these don't slow down shipping, they prevent entire bug categories
2. **Trust the CI pipeline** — 76 E2E assertions run in 8 min; engineers who trust it ship faster, not slower
3. **Calibrate risk** — UI label change ≠ debit/credit logic. Different scrutiny for different risk levels
4. **Question to ask before every PR:** *"If this code runs wrong in production, what's the worst that can happen?"*

---

# INTERVIEW 2 — Temu Java Engineer (International Payment System)

> **Role focus:** Cross-border payments, multi-currency, FX rates, compliance (KYC/AML), exactly-once execution.

---

## Q1 · Tell me about yourself

Same as Interview 1. Emphasize:
- PAT-FOS = payment/financial operations service → directly relevant
- Want to work on the harder version: multi-currency, FX, cross-border routing, settlement timing

---

## Q2 · PAT-FOS was single-currency. How would you extend it to multi-currency international payments?

**Three new problems single-currency doesn't have:**

**① Lock FX rate at authorization, not settlement:**
```java
public record PaymentRequest(
    UUID fromAccountId,
    String fromCurrency,       // "USD"
    BigDecimal fromAmount,
    UUID toAccountId,
    String toCurrency,         // "VND"
    BigDecimal lockedFxRate,   // stored on payment record at creation — never recalculated
    String idempotencyKey
)
```

**② Currency precision — BigDecimal scale per currency:**
Not all currencies use 2 decimal places. JPY = 0, USD = 2, KWD = 3.
Always: `.setScale(currency.scale, RoundingMode.HALF_UP)` — never default divide().

**③ Two-leg atomicity across systems = Saga, extended:**
```
Debit sender (FUNDS_HELD) → publish to Kafka
Routing service selects corridor (SWIFT / SEPA / local clearing)
Credit recipient asynchronously
Confirm both legs → SETTLED
Compensation if credit fails → REFUND_INITIATED
```

**New concept PAT-FOS didn't need:**
**Settlement window** — payment AUTHORIZED immediately, settlement T+0/T+1/T+2 depending on corridor. Data model must represent authorization state and settlement state independently.

---

## Q3 · Concrete scenario where BigDecimal precision goes wrong — and how to prevent it

**The bug — USD → JPY conversion:**
```java
BigDecimal jpy = new BigDecimal("50.00").multiply(new BigDecimal("149.85"));
// = 7492.50 — but JPY has ZERO decimal places
// Storing 7492.50 as JPY is wrong — ¥7492.50 doesn't exist

// Worse — forgetting RoundingMode:
BigDecimal usd = new BigDecimal("14985").divide(new BigDecimal("149.85"));
// ArithmeticException: Non-terminating decimal expansion — crashes production
```

**The fix — `Money` value object:**
```java
public record Money(BigDecimal amount, String currencyCode) {

    private static final Map<String, Integer> CURRENCY_SCALES = Map.of(
        "USD", 2, "EUR", 2, "JPY", 0, "KWD", 3, "VND", 0
    );

    // Always normalize scale on construction
    public Money {
        int scale = CURRENCY_SCALES.getOrDefault(currencyCode, 2);
        amount = amount.setScale(scale, RoundingMode.HALF_UP);
    }

    public Money convert(String targetCurrency, BigDecimal fxRate) {
        int targetScale = CURRENCY_SCALES.getOrDefault(targetCurrency, 2);
        BigDecimal converted = this.amount.multiply(fxRate)
            .setScale(targetScale, RoundingMode.HALF_UP);
        return new Money(converted, targetCurrency);
    }
}
```

**Rule:** scale is set exactly once, at the Money boundary. No raw BigDecimal arithmetic elsewhere.

---

## Q4 · International payments need KYC/AML. Some checks are sync, some async. How do you design the pipeline?

**Key insight: split by SLA requirement.**

**Synchronous gate (must pass before 202 is returned):**
- Sanctions check (OFAC list) → Redis cached → ~5ms → hard block if HIT → 403
- KYC status → DB flag read → ~10ms → if UNVERIFIED → 403

**Async path (after 202):**
- AML velocity check → aggregation over historical data → slow
- Third-party compliance API → external latency
- Behavioral anomaly detection → ML scoring

**Pipeline:**
```
POST /api/payments
  → ① Sanctions check (Redis)   → HIT → 403 immediately
  → ② KYC check (DB)            → UNVERIFIED → 403 immediately
  → ③ Lock FX rate               → store on payment record
  → ④ Debit hold (PostgreSQL)    → @Transactional
  → ⑤ Save AUTHORIZED            → publish to Kafka
  → ⑥ Return 202

Kafka → Compliance Service:
  → AML checks → CLEARED → routing → settlement
                → HOLD   → ops team review
                → REJECTED → cancel + refund
```

**Status state machine:**
```
PENDING → AUTHORIZED → COMPLIANCE_CLEARED → SETTLEMENT_PENDING → SETTLED
                     ↘ COMPLIANCE_HOLD
                     ↘ COMPLIANCE_REJECTED → REFUNDING → REFUNDED
```

**Bonus — compliance audit log fields:**
Every check result stored with: payment ID, country pair (USD→VND corridor), rule set version, timestamp. Regulators require 5-year retention.

---

## Q5 · Coding: Currency Conversion with Precision

**Problem:** Given transactions + exchange rates + currency scales, convert each transaction and return total per target currency.

**Clarify first:** Round each transaction individually (not sum-then-round). Standard in payments — customer sees per-transaction amounts.

```java
public Map<String, BigDecimal> convertAndSum(
        List<Transaction> transactions,
        Map<String, BigDecimal> rates,
        Map<String, Integer> scales) {

    Map<String, BigDecimal> totals = new HashMap<>();

    for (Transaction tx : transactions) {
        String rateKey = tx.fromCurrency() + "_" + tx.toCurrency();
        BigDecimal rate = rates.get(rateKey);
        if (rate == null) throw new IllegalArgumentException("No rate for: " + rateKey);

        int scale = scales.getOrDefault(tx.toCurrency(), 2);

        BigDecimal converted = tx.amount()
            .multiply(rate)
            .setScale(scale, RoundingMode.HALF_UP);  // ← round per transaction

        totals.merge(tx.toCurrency(), converted, BigDecimal::add);
    }
    return totals;
}
```

**Walk through example:**
- `100.00 USD × 149.85 = 14985.00` → scale 0 → **14985**
- `50.00 USD × 149.85 = 7492.50` → scale 0, HALF_UP → **7493**
- JPY total = **22478**
- `200.00 EUR × 1.08 = 216.00` → scale 2 → **216.00**

**Why explicit loop over stream:** stream hides the null check on `rate` — real production failure mode.

**Time:** O(n) · **Space:** O(k) where k = distinct target currencies

---

## Q6 · FX rates update every few seconds from market feed. How do you manage the cache?

**Why TTL-based cache (like PAT-FOS balances) doesn't work here:**
PAT-FOS balance TTL=60s is fine — source of truth is our own DB.
FX rates: source is external, 60s staleness = financial loss.

**Solution: background refresh + volatile reference swap**

```java
@Component
public class FxRateCache {

    // volatile ensures all threads see the updated reference immediately
    private volatile Map<String, BigDecimal> currentRates  = new ConcurrentHashMap<>();
    private volatile Map<String, BigDecimal> previousRates = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 5000)
    public void refresh() {
        try {
            Map<String, BigDecimal> fresh = marketDataClient.fetchLatestRates();
            previousRates = currentRates;  // save snapshot
            currentRates  = fresh;         // atomic reference swap — not key-by-key
        } catch (Exception e) {
            // serve previousRates — AP trade-off: stale > rejecting all payments
            if (staleDurationExceedsThreshold()) alertingService.send("FX stale > 30s");
        }
    }

    public BigDecimal getRate(String pair) {
        BigDecimal rate = currentRates.get(pair);
        if (rate == null) throw new FxRateNotFoundException("No rate: " + pair);
        return rate;
    }
}
```

**Why full map swap, not key-by-key update:**
Partial update means USD/JPY is new but EUR/USD is still old — a payment might get an inconsistent rate table. Swap the entire reference atomically.

**Cross-pod consistency:**
After each refresh, push canonical rates to Redis → all pods serve identical rates within the same 5-second window.

**Rate locking principle:**
Cache is for quoting only. Once customer initiates payment, rate is copied to the payment record and locked. Subsequent cache refreshes don't affect in-flight payments.

---

## Q7 · System Design: International Payment Processing System

**Requirements:**
- Cross-border money movement in local currencies
- Locked FX rate at authorization
- Exactly-once execution
- Full compliance audit trail
- p99 authorization < 500ms; settlement T+0 to T+2

**Architecture:**
```
Client → API Gateway → Payment Service (stateless pods)
  ↓
[Synchronous — < 500ms]
  ① Sanctions check (Redis OFAC list)
  ② KYC status (DB read)
  ③ FX rate lock (FxRateCache → stored on payment)
  ④ Debit hold (PostgreSQL @Transactional + pessimistic lock)
  ⑤ Save AUTHORIZED + publish to Kafka
  ⑥ Return 202

  ↓ Kafka (partitioned by paymentId)
[Async services]
  Compliance Service → AML checks → CLEARED or HOLD
  Routing Service    → select rail (SWIFT / SEPA / local clearing)
  Settlement Service → outbox pattern → call external rail
  Notification       → push/email both parties
```

**Exactly-once — three layers:**

| Layer | Mechanism |
|---|---|
| API | `idempotency_key UNIQUE` in PostgreSQL → constraint violation on retry → return existing |
| Kafka producer | `enable.idempotence=true` + `transactional.id` → broker deduplicates |
| Kafka consumer | Check `existsByPaymentId()` before processing → idempotent handler |

**Outbox pattern for external rail calls:**
```
Settlement Service:
  1. Write to outbox table in SAME PostgreSQL transaction as status update
  2. Outbox reader polls → calls external rail API
  3. On success: mark outbox processed
  4. On failure: retry with exponential backoff (max 3)
  5. After max retries: mark FAILED → trigger compensation (refund)
```
> "This guarantees the external call is attempted at least once without a distributed transaction between our DB and the external rail."

**Sharding:**
- Shard payments by `user_id` hash (32 shards)
- Snowflake IDs — globally unique, time-ordered, no coordination needed

**Alert on:**
- Authorization p99 > 300ms → synchronous path is slow
- Kafka consumer lag > 5 min → settlement backlog
- FX staleness > 30s → market data feed down
- Compliance hold rate > 5% → rule misconfiguration or attack

---

## Q8 · Behavioral: Customer says payment sent, sender debited, recipient never received money

**Investigation sequence:**

**Step 1 — Pull MongoDB audit trail for that payment ID.**
Last recorded state tells you where it stopped:
- `AUTHORIZED` but no `COMPLIANCE_CLEARED` → Kafka event never consumed by compliance service
- `COMPLIANCE_CLEARED` but no `SETTLEMENT_PENDING` → routing service dropped it
- `SETTLEMENT_PENDING` but no `SETTLED` → external rail call failed

**Step 2 — Check the outbox table.**
- Entry processed? → Rail confirmed delivery → issue is on receiving side
- Entry failed (max retries)? → Rail call failed silently → needs manual intervention
- Entry still pending? → Outbox processor is down → restart + alert

**Step 3 — Query the external rail directly.**
Every SWIFT/SEPA payment has a transaction reference. Look it up — did money leave our system?

**Step 4 — Correct action:**
- Money never left our system → re-execute settlement using same idempotency key (safe to retry)
- Money left, recipient bank didn't credit → escalate to rail
- No audit trail at all → P0 incident, escalate to engineering leadership

**Prevention:** Alert on outbox entries older than 10 minutes → PagerDuty. A lost payment should never sit undiscovered.

---

## Q9 · Behavioral: Speed vs quality in a financial system

**The framework:**

> "I don't think it's a real trade-off for the parts that matter."

**Protect the money path — no shortcuts:**
- Debit/credit logic, idempotency, state machine transitions
- These get reviewed carefully, have Testcontainers integration tests, need second approval
- A bug here costs real money — fixing in production is 10x more expensive

**Move fast everywhere else:**
- UI changes, dashboards, internal tooling, non-critical notifications
- These don't carry financial risk — perfectionism there is waste

**Question before every PR:**
> "If this code runs wrong in production, what's the worst that can happen?"
- "Customer loses money" → slow down
- "Admin sees stale dashboard" → ship it

**Automation as speed multiplier:**
The CI pipeline is not a bottleneck — it's what lets you merge confidently without re-testing manually. 76 assertions × 8 minutes = never ship the `AccessDeniedException → 500` bug.

---

# QUICK REFERENCE — Key Patterns Across Both Interviews

| Pattern | When to use | PAT-FOS example |
|---|---|---|
| Pessimistic lock | Money, high-contention writes | `@Lock(PESSIMISTIC_WRITE)` on account fetch |
| Optimistic lock | Low contention, read-heavy | Version column + retry |
| Redis atomic DECR | Inventory reservation at scale | Flash sale hot path |
| Saga (choreography) | Multi-service, no 2PC | Transfer → Kafka → Fraud → Kafka → Complete |
| Idempotency key | Prevent duplicate processing | UNIQUE constraint + catch DataIntegrityViolation |
| Outbox pattern | Reliable external calls | Settlement → outbox → external rail |
| Volatile map swap | Atomic config refresh | FX rate cache refresh |
| Money value object | Currency precision | `setScale(currencyScale, HALF_UP)` at boundary |
| Append-only audit log | Compliance, event history | MongoDB `transaction_events` |
| Cache-aside | Read performance | `@Cacheable` + `@CacheEvict` on balance |

---

# CONCURRENCY QUICK REFERENCE

```
Pessimistic locking  → SELECT FOR UPDATE → blocks concurrent readers
Optimistic locking   → version column → fails on conflict → retry
Redis DECR           → atomic, single-threaded → no contention
@Transactional       → REQUIRED (default): join or create
                     → REQUIRES_NEW: always new (suspends current)
Virtual Threads      → enable: spring.threads.virtual.enabled: true
                     → gotcha: synchronized pins VT to carrier → use ReentrantLock
```

---

# THINGS INTERVIEWERS SPECIFICALLY PRAISED

1. **FX rate lock at authorization time** — "that's a real bug that hits production systems"
2. **Rounding per-transaction vs sum-then-round distinction** — "you picked the right answer"
3. **Volatile map swap for FX cache** — "partial updates are dangerous in rate tables"
4. **Outbox table as first checkpoint for lost payments** — "that's where most bugs live"
5. **"Check-then-act breaks under concurrency"** — on idempotency question
6. **In-flight reservation window** — on Redis/MySQL reconciliation — "that's the detail most people miss"
7. **Jurisdiction tagging on audit records** — shows domain thinking beyond happy path

---

*Simulated: 2026-08-20 (Order System JD) + 2026-08-21 (International Payments JD)*
*Total questions covered: 18 | Sections: Technical deep-dives, Coding, System Design, Behavioral*
