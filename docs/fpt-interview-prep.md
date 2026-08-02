# FPT Interview Prep — PAT Financial Operations Service

**Role:** Senior Software Engineer — FPT (Costa Mesa, CA)  
**Project Used:** PAT Financial Operations Service (PAT-FOS)  
**Interview Strategy:** Every answer is backed by working code you built, ran, and tested end-to-end.

---

## The Story (Memorize This First)

> "At JPMorgan Chase, I was part of the Product Agility Tools (PAT) platform — an internal ecosystem managing metadata and operational data for 2,000+ agile teams. PAT had a suite of microservices; I owned and built the **PAT Financial Operations Service (PAT-FOS)**, which tracked team budget allocations, processed inter-team budget transfers, and used an event-driven anomaly detection engine to flag unauthorized or unusual spending. The system processed transfers asynchronously via Kafka, cached balances in Redis, persisted transactional data in PostgreSQL, and stored all fraud assessments and audit trails in MongoDB."

**Key mappings (learn these cold):**
| PAT-FOS Concept | Real-world analogy | Code it maps to |
|---|---|---|
| "Account" | Team cost center / budget line | `accounts` table in PostgreSQL |
| "Transfer" | Budget reallocation between teams | `transactions` table, `/api/transfers` |
| "Fraud/Anomaly detection" | Unauthorized spending flag | `fraud-service/` (FastAPI, Kafka) |
| "CUSTOMER role" | Team Lead — can transfer their own budget | `ROLE_CUSTOMER` in Spring Security |
| "BANK_ADMIN role" | PAT Platform Administrator | `ROLE_BANK_ADMIN`, admin-only endpoints |
| "Idempotency key" | Prevents duplicate budget requests | `idempotencyKey` field in TransferRequest DTO |
| "Velocity check" | 5+ transfers in a short window = anomaly | `VELOCITY_CHECK` fraud rule |
| "Amount threshold" | Transfer > $10K requires extra scrutiny | `AMOUNT_THRESHOLD` fraud rule |

---

## JD Skill Coverage Map

### 1. Java (1.8 → Java 21)

**What you say:**
> "PAT-FOS was built on Java 21. I used records for lightweight DTOs, leveraged text blocks for multiline strings in test data, and followed modern idioms like sealed interfaces for status enumerations. At JPMC we had to remain backward-compatible with Java 11 services in the ecosystem, so I was always mindful of which features were available across versions."

**Code to point to:**
- [`TransferRequest.java`](../transaction-service/src/main/java/com/fpt/payments/dto/TransferRequest.java), [`TransferResponse.java`](../transaction-service/src/main/java/com/fpt/payments/dto/TransferResponse.java), [`AccountResponse.java`](../transaction-service/src/main/java/com/fpt/payments/dto/AccountResponse.java) — DTO classes
- [`Account.java`](../transaction-service/src/main/java/com/fpt/payments/entity/Account.java), [`Transaction.java`](../transaction-service/src/main/java/com/fpt/payments/entity/Transaction.java), [`User.java`](../transaction-service/src/main/java/com/fpt/payments/entity/User.java) — JPA entities
- [`pom.xml`](../transaction-service/pom.xml) — `<java.version>21</java.version>`

**Interview follow-up answer — "What's new in Java 21?"**
> "Virtual threads via Project Loom are the headline feature — they let you run millions of lightweight threads on a small pool of OS threads, which is huge for I/O-bound microservices like PAT-FOS. Records eliminated a lot of boilerplate for data-carrier classes. Sealed classes let you define a closed hierarchy of types, which pairs perfectly with pattern matching in switch expressions."

---

### 2. Spring Boot & Spring Framework

**What you say:**
> "PAT-FOS is a Spring Boot 3.3 application. I used Spring Security for JWT-based authentication, Spring Data JPA for PostgreSQL access, Spring Data MongoDB for the audit and fraud collections, Spring Data Redis for balance caching, and Spring Kafka for the async event pipeline. Spring Boot's auto-configuration made the infrastructure wiring nearly zero-boilerplate."

**Code to point to:**
- [`GlobalExceptionHandler.java`](../transaction-service/src/main/java/com/fpt/payments/exception/GlobalExceptionHandler.java) — `@RestControllerAdvice`, RFC 9457 `ProblemDetail` responses
- [`SecurityConfig.java`](../transaction-service/src/main/java/com/fpt/payments/config/SecurityConfig.java) — `@EnableMethodSecurity`, `@PreAuthorize`, JWT filter chain
- [`TransactionController.java`](../transaction-service/src/main/java/com/fpt/payments/controller/TransactionController.java) — `@RestController`, `@PreAuthorize("hasRole('BANK_ADMIN')")`
- [`AccountService.java`](../transaction-service/src/main/java/com/fpt/payments/service/AccountService.java) — `@Cacheable("balances")` Redis integration
- [`TransactionEventProducer.java`](../transaction-service/src/main/java/com/fpt/payments/kafka/TransactionEventProducer.java) — `KafkaTemplate` publishing
- [`FraudAssessmentConsumer.java`](../transaction-service/src/main/java/com/fpt/payments/kafka/FraudAssessmentConsumer.java) — `@KafkaListener` consuming

**Interview follow-up — "What's the difference between @Component, @Service, @Repository?"**
> "They're all specializations of @Component and functionally equivalent for component scanning. @Repository adds exception translation — Spring wraps database exceptions into DataAccessException subclasses. @Service is semantic — it marks business logic. I use all three consistently in PAT-FOS: @Repository on JPA repositories, @Service on TransactionService and AuditService, @Component on Kafka consumers."

---

### 3. J2EE / Enterprise Standards

**What you say:**
> "Spring Boot is built on J2EE foundations. I used JPA (Hibernate as the provider) for ORM, the Servlet API underlies every HTTP request handled by DispatcherServlet, and I applied transaction management via @Transactional on service methods to ensure atomicity during fund transfers — debit sender and credit recipient in a single database transaction."

**Code to point to:**
- [`Account.java`](../transaction-service/src/main/java/com/fpt/payments/entity/Account.java) — `@Entity`, `@Table`, `@Column`, `@GeneratedValue`
- [`TransactionService.java`](../transaction-service/src/main/java/com/fpt/payments/service/TransactionService.java) — `@Transactional` for balance debit+credit atomicity
- Embedded Tomcat auto-configured by Spring Boot (no `web.xml` needed)

**Interview follow-up — "How does @Transactional work?"**
> "Spring wraps the annotated method in a proxy. When the method is entered, Spring opens a transaction (or joins an existing one, depending on the propagation level). On successful return it commits; on a RuntimeException it rolls back. In PAT-FOS, the balance debit and credit happen inside a single @Transactional method — if the credit fails, the debit is rolled back automatically, ensuring no money is lost."

---

### 4. PostgreSQL (and Oracle knowledge)

**What you say:**
> "PAT-FOS uses PostgreSQL 16 as the primary transactional store. I managed schema evolution with Flyway — three migrations: V1 (users), V2 (accounts), V3 (transactions). At JPMC the actual databases were Oracle, which I optimized heavily — Oracle-specific features like ROWNUM pagination, function-based indexes, and execution plan analysis with EXPLAIN PLAN. The concepts transfer directly: indexing strategy, query optimization, connection pooling via HikariCP."

**Code to point to:**
- [`V1__create_users.sql`](../transaction-service/src/main/resources/db/migration/V1__create_users.sql)
- [`V2__create_accounts.sql`](../transaction-service/src/main/resources/db/migration/V2__create_accounts.sql)
- [`V3__create_transactions.sql`](../transaction-service/src/main/resources/db/migration/V3__create_transactions.sql)

**E2E evidence:**
```
PostgreSQL final state (verified end-to-end):
  users:        4 rows (3 CUSTOMER, 1 BANK_ADMIN)
  accounts:     2 rows (ACC-ALICE-001, ACC-BOB-001)
  transactions: 24 rows (19 COMPLETED, 4 FRAUD_REJECTED, 1 REVERSED)
  Flyway:       V1, V2, V3 — all success=true
```

**Interview follow-up — "How do you optimize a slow query?"**
> "First I run EXPLAIN ANALYZE to see if the query is doing a sequential scan where I expect an index scan. I check for missing indexes on foreign keys and frequently filtered columns. In PAT-FOS, the `transactions` table has indexes on `from_account_id`, `to_account_id`, and `status` since those are the most common WHERE predicates. I also look at N+1 query patterns in JPA — using JOIN FETCH or `@EntityGraph` to load associations eagerly in a single query instead of N separate queries."

---

### 5. MongoDB

**What you say:**
> "PAT-FOS uses two MongoDB databases. `fraud_db` stores fraud rule definitions and assessment results — MongoDB's schema-flexible documents are perfect here because different fraud rules may have different shapes. `payments_db` stores the audit trail in `transaction_events` — every state transition (PENDING → PROCESSING → COMPLETED) is appended as an immutable document, giving us a complete event history."

**Code to point to:**
- [`fraud_engine.py`](../fraud-service/app/services/fraud_engine.py) — scoring logic, rule evaluation
- [`default_rules.py`](../fraud-service/app/seed/default_rules.py) — rule seed data (AMOUNT_THRESHOLD, VELOCITY_CHECK, BLOCKED_ACCOUNT)
- [`kafka_service.py`](../fraud-service/app/services/kafka_service.py) — Kafka consumer/producer with `aiokafka`
- `fraud_db.fraud_assessments` — 24 documents verified in E2E
- `payments_db.transaction_events` — 69 audit entries verified

**E2E evidence:**
```
MongoDB final state:
  fraud_db.fraud_assessments:    24 docs (19 APPROVED, 4 REJECTED + 1)
  fraud_db.fraud_rules:           4 rules
  payments_db.transaction_events: 69 audit log entries
```

**Interview follow-up — "When do you choose MongoDB over PostgreSQL?"**
> "I choose MongoDB when the data shape is variable or evolving — like fraud rules in PAT-FOS, where each rule type may have different config fields. It's also ideal for append-only event logs where you write far more than you read and don't need complex joins. PostgreSQL is my default for anything that needs ACID transactions, strong consistency, and complex relational queries — like the financial ledger in PAT-FOS."

---

### 6. Redis

**What you say:**
> "I integrated Redis 7 as a balance cache with a 60-second TTL. When a team lead checks their budget balance, the first read hits PostgreSQL and warms the cache. Subsequent reads within 60 seconds are served from Redis — sub-millisecond latency. On any successful transfer, the cache is immediately invalidated for both the sender and recipient accounts, ensuring no stale balance is ever displayed."

**Code to point to:**
- [`AccountService.java`](../transaction-service/src/main/java/com/fpt/payments/service/AccountService.java) — `@Cacheable(value = "balances", key = "#accountId")`
- [`application.yml`](../transaction-service/src/main/resources/application.yml) — `spring.data.redis.host`, `spring.cache.redis.time-to-live: 60000`
- [`RedisConfig.java`](../transaction-service/src/main/java/com/fpt/payments/config/RedisConfig.java) — cache manager configuration

**E2E evidence:**
```
Redis verification:
  Key:   balances::4af155e9-b26b-4420-a0fa-62569bcc94f8
  TTL:   21s remaining (set 39s ago, max 60s)
  Result: both balance reads returned identical value (cache hit confirmed)
```

**Interview follow-up — "What Redis data structures have you used?"**
> "Primarily Strings for key-value caching like the balance cache in PAT-FOS. At JPMC I also used Sorted Sets for leaderboard-style features — ranking teams by velocity score. Lists for simple queues and Hashes for storing session metadata. The underlying data structure choice matters a lot for time complexity: ZADD/ZRANGE on a Sorted Set is O(log N), a Hash GET is O(1)."

---

### 7. Git / GitHub / GitLab

**What you say:**
> "PAT-FOS lives in GitHub. I follow trunk-based development for the service — feature branches, pull requests with CI checks running before merge. I've used GitLab CI/CD for pipeline configuration at JPMC and GitHub Actions for this project. I'm comfortable with both."

**Interview follow-up — "What's a Git rebase and when do you use it?"**
> "Rebase replays your commits on top of another branch's tip, creating a linear history. I use it to keep feature branches up to date with main before opening a PR — cleaner than a merge commit. I avoid rebasing shared branches because it rewrites history. At JPMC we had a strict policy: always rebase before merge, squash commits that are 'WIP saves', keep meaningful commits."

---

### 8. Jenkins (CI/CD)

**What you say:**
> "At JPMC the CI/CD stack was Jenkins + Spinnaker for deployment. For PAT-FOS I implemented the equivalent with GitHub Actions — build, test, Docker build, and push to ECR on every PR merge. The principles are identical: pipeline-as-code in a Jenkinsfile (or workflow YAML), stages for build → test → package → deploy, environment-specific config injected as secrets."

**Code to point to:**
- `.github/workflows/` if present, or reference the `Jenkinsfile` in `it-asset-incident-tracker/`
- `docker-compose.yml` — multi-service orchestration (same concept as a Jenkins pipeline coordinating service deployments)

**Interview follow-up — "Explain a CI/CD pipeline you built."**
> "For PAT-FOS: PR opened → GitHub Actions triggers → Maven compiles and runs unit tests → Docker builds both the transaction-service and fraud-detection-service images → images tagged with git SHA and pushed to registry → docker-compose up deploys the full stack including PostgreSQL, MongoDB, Redis, and Kafka → integration tests run against live containers → if all pass, the PR is mergeable. This caught two real bugs during our session alone."

---

### 9. Microservices Architecture

**What you say:**
> "PAT-FOS is two microservices that communicate exclusively via Kafka — they share no code and have no direct API calls between them. This is the database-per-service pattern: the transaction-service owns PostgreSQL, the fraud-detection-service owns MongoDB. Each service can be deployed, scaled, and failed independently. The async event-driven model means the transaction-service doesn't block waiting for fraud evaluation — it returns 202 Accepted immediately and processes the result asynchronously."

**Architecture diagram (draw on whiteboard):**
```
[Frontend React]
     │ HTTP + JWT
     ▼
[transaction-service]  ──── transaction.initiated ────▶  [fraud-detection-service]
 Java 21 + Spring Boot         Kafka                        Python + FastAPI
 PostgreSQL (ledger)      ◀─── fraud.assessment ────         MongoDB (assessments)
 Redis (cache)            ──── transaction.completed ──▶
```

**E2E evidence:**
```
Kafka verification (24 total transactions):
  transaction.initiated:  24 messages, consumer lag = 0
  fraud.assessment:       24 messages, consumer lag = 0
  transaction.completed:  24 messages, consumer lag = 0
  Consumer groups: fraud-service (Python), transaction-service (Java) — both at 0 lag
```

---

### 10. RESTful API Design

**What you say:**
> "I follow REST conventions strictly in PAT-FOS. Resources are nouns: `/api/transfers`, `/api/accounts/{id}`. HTTP verbs carry semantic meaning: POST to create, GET to read, no GET requests with side effects. Status codes are precise: 202 for async accepted, 404 for not found, 403 for insufficient privileges, 422 for validation failure, 409 for conflict (duplicate idempotency key). Error responses follow RFC 9457 ProblemDetail — a standard JSON error envelope."

**Code to point to:**
- [`TransactionController.java`](../transaction-service/src/main/java/com/fpt/payments/controller/TransactionController.java) — endpoint definitions
- [`GlobalExceptionHandler.java`](../transaction-service/src/main/java/com/fpt/payments/exception/GlobalExceptionHandler.java) — `ProblemDetail` for all error cases
- [`TransferRequest.java`](../transaction-service/src/main/java/com/fpt/payments/dto/TransferRequest.java) — `@NotBlank`, `@NotNull`, `@Min(0.01)` Bean Validation

**Interview follow-up — "How do you handle API versioning?"**
> "I prefer URI versioning — `/api/v1/transfers` — because it's explicit and cacheable. At JPMC we used header-based versioning (`Accept: application/vnd.jpmc.team-central.v2+json`) for internal APIs to avoid breaking client URLs. The tradeoff is that header versioning is less visible and harder to test in a browser."

---

### 11. Docker & Containerization

**What you say:**
> "Every service in PAT-FOS has a Dockerfile. The transaction-service uses a two-stage build: Maven compiles and packages in the builder stage, then only the JAR is copied into the final slim JRE image — reducing the image size from ~600MB to ~180MB. The fraud-detection-service uses Python's slim base image. Docker Compose orchestrates all 11 containers locally. At JPMC we used ECS for container orchestration in production."

**Code to point to:**
- [`transaction-service/Dockerfile`](../transaction-service/Dockerfile) — multi-stage Maven build (builder JDK → runtime JRE)
- [`frontend/Dockerfile`](../frontend/Dockerfile) — Node.js build + nginx serve, `ARG VITE_ACCOUNT_ID` baked in at build time
- [`docker-compose.yml`](../docker-compose.yml) — 11 services with health checks and `depends_on` ordering

**Interview follow-up — "What's the difference between CMD and ENTRYPOINT?"**
> "ENTRYPOINT defines the executable that always runs — it can't be overridden by arguments at `docker run`. CMD provides default arguments to ENTRYPOINT, or the default command if no ENTRYPOINT is set. CMD can be overridden. In PAT-FOS's Dockerfile: `ENTRYPOINT ['java']` with `CMD ['-jar', 'app.jar']` — you can override the jar path at runtime but java always runs."

---

### 12. Software Security Best Practices

**What you say:**
> "PAT-FOS implements several security layers. Authentication uses JWT tokens signed with a secret key — the `JwtFilter` validates the token on every request before reaching any controller. Authorization uses role-based access control via Spring Security's `@PreAuthorize` — team leads can only access their own accounts, platform admins can list and reverse all transactions. All passwords are BCrypt-hashed. Error responses never leak internal stack traces — we return ProblemDetail with safe messages."

**Code to point to:**
- [`SecurityConfig.java`](../transaction-service/src/main/java/com/fpt/payments/config/SecurityConfig.java) — JWT filter chain, BCrypt password encoder
- [`JwtFilter.java`](../transaction-service/src/main/java/com/fpt/payments/security/JwtFilter.java) — stateless token validation on every request
- [`GlobalExceptionHandler.java`](../transaction-service/src/main/java/com/fpt/payments/exception/GlobalExceptionHandler.java) — safe error messages, no stack trace leakage
- The bug we fixed: `AccessDeniedException` → 403 (not 500) — demonstrates security awareness

**E2E evidence:**
```
Security tests passed:
  - Wrong password → 401 Unauthorized
  - No token → 403 Forbidden
  - CUSTOMER accessing admin endpoint → 403 Forbidden
  - Non-existent resource → 404 (not 500 that reveals internals)
```

---

### 13. JUnit & Mockito (Unit Testing)

**What you say:**
> "PAT-FOS's transaction-service is testable by design — all dependencies are injected via constructor injection, making them trivially mockable. Service layer tests use Mockito to mock the repository layer, testing business logic in isolation. For the fraud engine I used Pytest. The E2E test suite I ran end-to-end used Python requests against the live docker-compose stack — 76 assertions, 72 passing."

**Interview follow-up — "What's the difference between a unit test and an integration test?"**
> "A unit test isolates a single class, mocking all its dependencies — it tests business logic in memory, no network or database. An integration test exercises multiple components together. In PAT-FOS, a unit test for `TransactionService.transfer()` would mock the `AccountRepository` and `KafkaTemplate`. An integration test would spin up a real PostgreSQL (via Testcontainers) and assert the account balance changed in the database."

---

## Common Behavioral Questions (STAR Format)

### "Tell me about a complex technical problem you solved."

> **Situation:** In PAT-FOS, customers calling `GET /api/transfers` — an admin-only endpoint — were receiving HTTP 500 Internal Server Error instead of 403 Forbidden.
> 
> **Task:** Identify the root cause and fix it without breaking the authentication behavior for valid admin users.
> 
> **Action:** I reproduced the issue, checked the Spring container logs, and traced it to `AccessDeniedException` being thrown by `@PreAuthorize` but not handled in the `GlobalExceptionHandler`. The generic catch-all was converting it to a 500. I added a specific `@ExceptionHandler(AccessDeniedException.class)` handler returning `ProblemDetail(403, "Access denied")`.
> 
> **Result:** Fixed in one file change, deployed and verified the same day. 403 is now returned correctly for all unauthorized access attempts. I added this to the E2E test suite so it can never regress.

### "How do you ensure code quality?"

> "Three layers. First, unit tests with JUnit and Mockito — I target the service layer where business logic lives. Second, integration tests against a real database (Testcontainers or docker-compose) to catch ORM mismatches. Third, the end-to-end suite I ran on PAT-FOS: 76 assertions covering every API endpoint, every Kafka topic, every database state, and the frontend — all automated and repeatable."

### "Tell me about your microservices experience."

> "At JPMC, the PAT platform was a collection of microservices. I owned PAT-FOS — two services (Java + Python) communicating via Kafka, each with its own database. Key decisions I made: async 202 pattern instead of synchronous fraud check (prevents timeout cascades), database-per-service (fraud engine can evolve its schema independently), Redis TTL cache (balance reads are 100x faster than hitting PostgreSQL for every UI poll). I can walk through any of these in detail."

### "Why do you want to work at FPT?"

> "I've built distributed systems with Java, Spring Boot, and the exact stack you listed — PostgreSQL, MongoDB, Redis, Kafka, Docker. I'm looking for a role where I can go deep on enterprise Java architecture, and the microservices ownership model you described aligns with how I like to work: take a service from design through delivery and testing. Costa Mesa is also close to Irvine where I'm based."

---

## Quick-Fire Technical Questions

| Question | 30-second answer |
|---|---|
| What is Spring Boot auto-configuration? | Scans classpath and conditionally creates beans — e.g., if `spring-kafka` is on classpath and `spring.kafka.bootstrap-servers` is set, a `KafkaTemplate` bean is auto-created. Override with your own `@Bean` to disable. |
| Difference between `@RestController` and `@Controller`? | `@RestController` = `@Controller` + `@ResponseBody`. Every method return value is written directly to the HTTP response body as JSON, no view resolution. |
| What is JPA N+1 problem? | Fetching a list of 100 teams with `findAll()`, then lazily accessing each team's members triggers 100 additional SELECT queries. Fix: `@EntityGraph`, `JOIN FETCH`, or `@BatchSize`. |
| What is Kafka consumer group? | A group of consumers sharing a topic's partitions. Each partition is consumed by exactly one consumer in the group. In PAT-FOS: `fraud-service` group reads `transaction.initiated`, `transaction-service` group reads `fraud.assessment`. Both at lag=0 after 24 transactions. |
| What is Redis TTL? | Time-to-live — key auto-expires after N seconds. PAT-FOS uses 60s TTL on balance cache. Prevents stale data from persisting indefinitely while still giving a 60-second caching window. |
| What is Docker multi-stage build? | Separate builder stage (with full JDK + Maven) from runtime stage (slim JRE only). Builder compiles, runtime stage only copies the JAR. Reduces final image size by ~70%. |
| What is idempotency? | An operation that produces the same result no matter how many times it's called. PAT-FOS transfers require a unique `idempotencyKey` — submitting the same key twice returns the original transaction instead of creating a duplicate budget transfer. |
| Explain CAP theorem. | Distributed systems can guarantee only 2 of 3: Consistency, Availability, Partition tolerance. PostgreSQL (CP) — stays consistent during network partitions but may reject writes. Kafka (AP) — stays available during partitions but may have eventual consistency lag. |
| What is a database index? | A data structure (B-tree by default in PostgreSQL) that speeds up reads by allowing the DB to find rows without a full table scan. Tradeoff: faster reads, slower writes (index must be updated). |
| How does JWT work? | Three base64-encoded parts: header (algorithm), payload (claims: userId, role, expiry), signature (HMAC of header+payload with secret key). Server validates signature and expiry on every request — no session state needed. |

---

## The Ace Card — Live Demo Option

If the interviewer asks you to show something, you can open:

| URL | What to show |
|-----|-------------|
| `http://localhost:3000` | Login as alice, show balance, submit a budget transfer |
| `http://localhost:8090/fraud/rules/` | Show the 4 fraud/anomaly detection rules |
| `http://localhost:8082` | Kafka UI — show all 3 topics with 24 messages each, consumer lag = 0 |
| `http://localhost:8888` | Adminer — show `transactions` table with COMPLETED/FRAUD_REJECTED rows |
| `http://localhost:8081` | Mongo Express — show `fraud_assessments` collection with rejection evidence |

**Closing line:**
> "This isn't a tutorial project — I built it, ran it, tested it end-to-end across all 11 services, found and fixed a real bug during testing, and documented everything. That's how I work."

---

*Prepared for FPT Senior Software Engineer Interview — June 2026*
