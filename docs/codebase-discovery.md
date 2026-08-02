# PAT-FOS Codebase Discovery — New-Hire Walkthrough Guide

**Project:** PAT Financial Operations Service (PAT-FOS)  
**Prepared by:** GitHub Copilot — Live Codebase Discovery Session  
**Date:** 2026-06-25

---

## Step 1 — Discovery Results

### 1A — Language and Runtime
| Service | Language | Runtime Version |
|---|---|---|
| `transaction-service` | Java | **21** (eclipse-temurin) — declared in `pom.xml` `<java.version>21</java.version>` |
| `fraud-service` | Python | **3.12** — declared in `.github/workflows/ci.yml` `python-version: "3.12"` |
| `frontend` | TypeScript | **5.4.5** — declared in `frontend/package.json` |

### 1B — Frameworks and Libraries
| Layer | Technology |
|---|---|
| Java web framework | Spring Boot **3.3.0** |
| Java ORM | Spring Data JPA / Hibernate (dialect: PostgreSQLDialect) |
| Java security | Spring Security + jjwt **0.12.6** (HMAC-SHA) |
| Java messaging | Spring Kafka (producer + consumer) |
| Java cache | Spring Cache + Spring Data Redis |
| Java migration | Flyway **flyway-database-postgresql** |
| Java docs | springdoc-openapi **2.5.0** → Swagger UI |
| Java utilities | Lombok (code generation) |
| Python web framework | FastAPI **0.111.0** |
| Python MongoDB driver | motor **3.6.0** (async) |
| Python Kafka client | aiokafka **0.11.0** |
| Python validation | Pydantic **v2** (2.7.1) |
| Python testing | pytest + pytest-asyncio + pytest-mock |
| Python linting | ruff 0.4.7 |
| Frontend framework | React **18.3.1** + TypeScript + React Router **6** |
| Frontend build | Vite **5.3.1** |
| Frontend styling | Tailwind CSS **3.4.4** |
| Frontend HTTP client | Axios **1.7.2** |
| Frontend component catalog | Storybook **8.1.10** (scripts present, no stories written yet) |

### 1C — Build and Package Management
| Service | Build Tool | Dependency File |
|---|---|---|
| `transaction-service` | Maven 3.9 | `transaction-service/pom.xml` |
| `fraud-service` | pip | `fraud-service/requirements.txt` |
| `frontend` | npm | `frontend/package.json` |

### 1D — Database and Storage
| Database | Type | Version | What it stores |
|---|---|---|---|
| PostgreSQL | Relational (ACID) | 16-alpine | `users`, `accounts`, `transactions` |
| MongoDB | Document (AP) | 7 | `transaction_events` (audit), `fraud_assessments`, `fraud_rules` |
| Redis | Key-value cache | 7-alpine | `balances::{accountId}` — 60-second TTL |

**Migrations (Flyway):**
- `V1__create_users.sql` — `users` table + seeds 3 users (admin, alice, bob)
- `V2__create_accounts.sql` — `accounts` table + seeds 2 accounts (Alice $50K, Bob $20K)
- `V3__create_transactions.sql` — `transactions` table + 4 indexes

### 1E — Async and Event-Driven
| Tool | Version | Kafka Topics |
|---|---|---|
| Apache Kafka (Confluent) | 7.6.1 | `transaction.initiated` (Java → Python), `fraud.assessment` (Python → Java) |
| Zookeeper | 7.6.1 | Kafka cluster coordinator |

No scheduled jobs or cron tasks found.

### 1F — API Style
- **REST** (JSON over HTTP) for all three services
- **OpenAPI/Swagger UI**: `transaction-service` at `/swagger-ui.html` and `/v3/api-docs`
- **FastAPI auto-docs**: `fraud-service` at `/docs`
- No GraphQL, gRPC, or WebSocket

### 1G — CI/CD Pipeline
- **System:** GitHub Actions
- **Config:** `.github/workflows/ci.yml`
- **Trigger:** push to `main`/`develop`, PR to `main`
- **5 jobs:**
  ```
  test-java ──┐
  test-python ─┼── integration-tests ── build-and-push (main only → GHCR)
  test-frontend┘
  ```

### 1H — Containerization and Orchestration
| File | Type | Purpose |
|---|---|---|
| `transaction-service/Dockerfile` | Multi-stage (Maven builder → JRE runtime) | Java service image ~85MB |
| `fraud-service/Dockerfile` | Single-stage (python:3.12-slim) | Python service image |
| `frontend/Dockerfile` | Multi-stage (Node builder → nginx runtime) | Static SPA + reverse proxy |
| `docker-compose.yml` | 11 services | Full local dev stack |

No Kubernetes manifests found.

### 1I — Cloud Infrastructure
No Terraform, AWS SDK, Azure SDK, or GCP SDK found in the codebase.  
The CI pipeline pushes Docker images to **GitHub Container Registry (GHCR)**.  
Cloud deployment (ECS Fargate) is documented in `docs/Q&A-interview.md` but not implemented in this repo.  
Production deployment target for quick demos: **Render** (referenced in workspace-level docs).

### 1J — Observability
- **Logging:** SLF4J/Logback via `@Slf4j` (Lombok) — standard Spring Boot JSON logs
- **Metrics/health:** Spring Actuator — `/actuator/health` and `/actuator/info` exposed
- **No distributed tracing:** No Zipkin, Jaeger, or OpenTelemetry
- **No metrics export:** No Prometheus endpoint configured

### 1K — Security
- **JWT auth:** HMAC-SHA256 key from `${JWT_SECRET}` env var (jjwt 0.12.6)
- **Password hashing:** BCryptPasswordEncoder
- **RBAC:** Two roles — `CUSTOMER` and `BANK_ADMIN`, enforced via `@PreAuthorize`
- **Secrets:** Environment variables via Docker Compose / `.env` file
- **CSRF:** Disabled (stateless API, no cookies)
- **Sessions:** Stateless — `SessionCreationPolicy.STATELESS`

### 1L — Documentation and DX
- `README.md` — project overview + run instructions
- `.env.example` — all environment variables documented
- `docs/` — interview prep materials, e2e reports, service tour
- OpenAPI at `/swagger-ui.html` (transaction-service)
- FastAPI auto-docs at `http://localhost:8090/docs` (fraud-service)
- Storybook configured (scripts present) — no story files written yet
- No Makefile or Taskfile

---

## Step 2 — Custom Phase Plan

> All phases are based exclusively on files found in this project. Phase names use the actual technologies discovered.

| # | Phase Name | Files Covered | Why This Order |
|---|---|---|---|
| 0 | **Welcome** | Top-level tree | Orient the new hire |
| 1 | **Spring Boot Bootstrap — pom.xml, application.yml, PaymentTransactionApplication** | `pom.xml`, `application.yml`, `PaymentTransactionApplication.java` | *What is this app and what does it declare it needs?* — smallest, most readable starting point |
| 2 | **Flyway Migrations — PostgreSQL Schema (V1/V2/V3)** | `db/migration/V1-V3`, seed data | *What data does it manage?* — plain SQL, zero framework knowledge needed |
| 3 | **JPA Entities and Repositories — User, Account, Transaction** | `entity/`, `repository/`, `enums/TransactionStatus.java` | *How does Java model that data?* — builds directly on the SQL from Phase 2 |
| 4 | **TransactionService and the TransactionStatus State Machine** | `service/TransactionService.java`, `enums/TransactionStatus.java`, `service/AuthService.java`, `service/AccountService.java` | *What is the most important thing this app does?* — core business logic |
| 5 | **REST API Controllers — AuthController, AccountController, TransactionController** | `controller/`, `dto/` | *How do users trigger that logic?* — public surface area, endpoints, RBAC |
| 6 | **Spring Security + JWT — SecurityConfig, JwtUtil, JwtFilter** | `config/SecurityConfig.java`, `security/JwtUtil.java`, `security/JwtFilter.java` | *Who is allowed in and how is that checked?* |
| 7 | **GlobalExceptionHandler and Bean Validation — RFC 9457 ProblemDetail** | `exception/GlobalExceptionHandler.java`, `dto/TransferRequest.java`, all exception classes | *What happens when something goes wrong?* — cross-cutting concern |
| 8 | **Kafka Integration — Topics, Producer, Consumer, Event Records** | `config/KafkaProducerConfig.java`, `config/KafkaConsumerConfig.java`, `kafka/`, `event/` | *Why is the transfer async? What crosses the wire?* |
| 9 | **Redis Cache — RedisConfig, @Cacheable on AccountService** | `config/RedisConfig.java`, `service/AccountService.java` | *How is the DB protected from read load?* |
| 10 | **MongoDB Audit Layer — TransactionEvent, AuditService** | `mongo/TransactionEvent.java`, `service/AuditService.java`, `repository/TransactionEventRepository.java` | *Why is a second database needed?* |
| 11 | **Python Fraud Detection Service — FastAPI, fraud_engine.py, kafka_service.py** | `fraud-service/` entirely | *What lives on the other end of that Kafka event?* |
| 12 | **React/TypeScript Frontend — Routing, transactionApi.ts, AccountDashboard, TransferForm** | `frontend/src/` entirely, `nginx.conf` | *How does a human actually use all of this?* |
| 13 | **JUnit 5 + Mockito Tests and pytest Tests** | `src/test/`, `fraud-service/tests/` | *What is proven? What is missing?* |
| 14 | **Multi-stage Dockerfiles — transaction-service, fraud-service, frontend** | all three `Dockerfile`s | *How does each service get packaged?* |
| 15 | **docker-compose.yml and .env.example — 11-Service Local Stack** | `docker-compose.yml`, `.env.example` | *Now you know every service — this YAML makes complete sense* |
| 16 | **GitHub Actions CI Pipeline — ci.yml** | `.github/workflows/ci.yml` | *How does a commit become a verified image?* |
| 17 | **Wrap-Up — End-to-End Transfer Trace, What's Missing, Open Q&A** | All layers | Synthesize everything; identify gaps |

---

## Session Log

---

## Phase 1 — Spring Boot Bootstrap: `pom.xml`, `application.yml`, `PaymentTransactionApplication.java`

**What this phase covers:** How the application declares itself to the world — what libraries it pulls in, what configuration it needs to start, and what the JVM runs first.

---

### File 1: `transaction-service/pom.xml`

This is the Maven build file. It does three things: declares what this project **is**, what it **depends on**, and how to **build and test it**.

#### What this project is

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
</parent>

<artifactId>transaction-service</artifactId>
<description>Payment Transaction Service — Spring Boot / Java 21</description>
```

`spring-boot-starter-parent` is the master configuration. It sets default compiler options, plugin versions, and a curated set of compatible dependency versions — so you don't specify versions for most Spring dependencies; the parent already knows what's compatible with 3.3.0.

#### Runtime version pin

```xml
<java.version>21</java.version>
```

One line. Every plugin and compiler configuration in the parent reads this property. If you change it to `17`, the whole build switches.

#### Dependencies — grouped by purpose

**① Web layer**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
Bundles: embedded Tomcat + Spring MVC + Jackson (JSON serialization). Everything needed to receive an HTTP request and return a JSON response.

**② Security**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```
Activates the Spring Security filter chain. The moment this is on the classpath, *every endpoint is locked by default* — nothing is accessible without auth unless you explicitly permit it.

**③ PostgreSQL (JPA + driver)**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```
`data-jpa` = Hibernate ORM + Spring Data repositories. The `postgresql` driver is `scope: runtime` — it's not needed at compile time, only when the app actually runs and connects to the database.

**④ MongoDB**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```
Adds a second persistence layer — Spring Data MongoDB. This is for the audit event log only, completely separate from JPA.

**⑤ Redis**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```
Enables `@Cacheable` / `@CacheEvict` to route through Redis. Without this, those annotations do nothing.

**⑥ Kafka**
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```
Enables `@KafkaListener` consumers and `KafkaTemplate` producers.

**⑦ Flyway (database migrations)**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```
On every startup, Flyway checks `db/migration/` and applies any SQL scripts that haven't run yet. The second artifact is the PostgreSQL dialect plugin — required since Flyway 10.

**⑧ JWT**
```xml
<artifactId>jjwt-api</artifactId>     <!-- compile-time interface -->
<artifactId>jjwt-impl</artifactId>    <!-- runtime: actual crypto -->
<artifactId>jjwt-jackson</artifactId> <!-- runtime: JSON serialization for claims -->
```
Three separate jars because the API, implementation, and JSON support are split. `jjwt-impl` is `scope: runtime` — you never reference its classes directly; you only call the API interfaces.

**⑨ Bean Validation**
```xml
<artifactId>spring-boot-starter-validation</artifactId>
```
Enables `@NotNull`, `@NotBlank`, `@DecimalMin` on DTO fields. Without this, `@Valid` on a controller parameter is silently ignored.

**⑩ OpenAPI / Swagger**
```xml
<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
<version>2.5.0</version>
```
Inspects all `@RestController` classes at startup and generates a live Swagger UI at `/swagger-ui.html`. You don't write any YAML — it reads your annotations.

**⑪ Lombok**
```xml
<artifactId>lombok</artifactId>
<optional>true</optional>
```
`optional: true` means it's a compile-time annotation processor, not a runtime dependency. The generated bytecode (getters, constructors, builders) is in the compiled `.class` files. Nothing Lombok-related ships in the final JAR.

**⑫ Actuator**
```xml
<artifactId>spring-boot-starter-actuator</artifactId>
```
Adds `/actuator/health` — used by Docker health checks, Kubernetes readiness probes, and load balancers to decide whether to send traffic to this instance.

---

### File 2: `src/main/resources/application.yml`

This file binds environment variables to Spring configuration keys. The pattern `${ENV_VAR:default}` means: read from the environment, fall back to the default if not set.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/${POSTGRES_DB:payments_db}
    username: ${POSTGRES_USER:payments_user}
    password: ${POSTGRES_PASSWORD:payments_pass}
```
Locally, defaults let you run without setting anything. In Docker Compose, the container injects `POSTGRES_HOST=postgres` (the container name). In AWS ECS, those values come from Secrets Manager.

```yaml
  jpa:
    hibernate:
      ddl-auto: validate
```
`validate` is the production-safe setting. Hibernate reads the database schema and *checks* that it matches the entity definitions — it never creates or alters tables. If there's a mismatch, the app refuses to start. Schema changes are Flyway's job, not Hibernate's.

```yaml
  flyway:
    enabled: true
    locations: classpath:db/migration
```
Tells Flyway exactly where to find the SQL scripts. `classpath:` means inside the compiled JAR, not the filesystem.

```yaml
  data:
    mongodb:
      uri: mongodb://${MONGO_HOST:localhost}:27017/${MONGO_DB:payments_db}
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
```
Two separate data sources — MongoDB and Redis — configured independently. Spring auto-creates the clients from these properties.

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:bXlTdXBlclNlY3...}
    expiration-ms: 86400000   # 24 hours
  cache:
    balance-ttl-seconds: 60
```
Custom namespace `app.*`. These are not Spring built-ins — they're read by `JwtUtil` (`@Value("${app.jwt.secret}")`) and `RedisConfig` (`@Value("${app.cache.balance-ttl-seconds:60}")`). The default JWT secret is a base64-encoded string — safe for local dev, **must be overridden in production**.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```
Only `health` and `info` are exposed over HTTP. Everything else (metrics, env, beans, heap dump) is locked off. This is the secure default.

---

### File 3: `PaymentTransactionApplication.java`

```java
@SpringBootApplication
@EnableCaching
@EnableKafka
@OpenAPIDefinition(...)
@SecurityScheme(name = "bearerAuth", ...)
public class PaymentTransactionApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentTransactionApplication.class, args);
    }
}
```

`@SpringBootApplication` is shorthand for three annotations:
- `@SpringBootConfiguration` — this class is a config source
- `@EnableAutoConfiguration` — Spring reads the classpath and auto-configures beans (sees Redis on classpath → creates `RedisConnectionFactory` automatically)
- `@ComponentScan` — scans `com.fpt.payments` and all sub-packages for `@Service`, `@Repository`, `@Controller`, etc.

`@EnableCaching` activates the `@Cacheable` / `@CacheEvict` proxy infrastructure. Without it, those annotations compile and run silently but do nothing.

`@EnableKafka` activates `@KafkaListener` scanning. Same story — without it, the consumer annotations are dead code.

The `@OpenAPIDefinition` and `@SecurityScheme` are metadata for Swagger UI only — they add the "Authorize" button and bearer token input to the `/swagger-ui.html` page.

#### What happens when `main()` runs

1. Spring scans all packages → discovers every `@Component`, `@Service`, `@Repository`, `@Controller`
2. Auto-configuration fires → creates `DataSource`, `EntityManagerFactory`, `RedisConnectionFactory`, `KafkaTemplate`, etc.
3. Flyway runs → checks `db/migration/`, applies any pending V1/V2/V3 scripts
4. Hibernate validates the schema → confirms tables match entities
5. Kafka consumers register their listeners
6. Tomcat starts → port 8080 is open
7. First health check responds: `{"status":"UP"}`

#### What breaks if you remove each annotation

| Remove | Effect |
|---|---|
| `@EnableCaching` | `@Cacheable` on `AccountService.getBalance()` silently stops caching — no error, just DB hit on every call |
| `@EnableKafka` | `FraudAssessmentConsumer` never receives messages — transfers stay `PENDING_FRAUD_CHECK` forever |
| `@SpringBootApplication` | Application doesn't start |

---

### Phase 1 Challenge Question

`application.yml` has `ddl-auto: validate`. You add a new field `String nickname` to the `Account` entity but don't create a Flyway migration. What happens when the app starts?

> **Answer:** Flyway runs first and sees nothing new to apply. Then Hibernate validates — it finds the entity has a `nickname` column that doesn't exist in the `accounts` table. Spring Boot throws a `SchemaManagementException` and the application **refuses to start**. The fix: add a new `V4__add_account_nickname.sql` with `ALTER TABLE accounts ADD COLUMN nickname VARCHAR(100) NULL`.

---

## Phase 2 — Flyway Migrations: PostgreSQL Schema (V1/V2/V3)

**What this phase covers:** The three SQL files that define every table, constraint, index, and seed record in the PostgreSQL database. These run before a single line of Java application code executes.

---

### How Flyway Works

Flyway is a database migration tool. On every startup it:

1. Connects to PostgreSQL using the datasource from `application.yml`
2. Checks (or creates) a table called `flyway_schema_history` — its internal ledger of what has already run
3. Scans `classpath:db/migration` for files matching `V{n}__{description}.sql`
4. Runs any version it hasn't seen before, in version order
5. Records the checksum of each applied file — if you edit an already-applied file, Flyway refuses to start with a checksum error

The naming rule is strict: **capital V**, version number, **double underscore**, description, `.sql`. The double underscore is the separator — one underscore in the description is fine.

---

### File 1: `V1__create_users.sql`

```sql
CREATE TABLE IF NOT EXISTS users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'CUSTOMER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

**Column by column:**

`id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
— UUID primary key generated by PostgreSQL itself, not by Java. `gen_random_uuid()` produces a cryptographically random v4 UUID. Using UUID instead of a serial integer means: no sequential enumeration (attacker can't guess `user/2`, `user/3`), works across distributed systems, and IDs are globally unique.

`email VARCHAR(255) NOT NULL UNIQUE`
— The natural identity of a user. `UNIQUE` creates an implicit B-tree index — `findByEmail()` is O(log n) without any explicit `CREATE INDEX`. `NOT NULL` enforced at the DB level, not just application level.

`password_hash VARCHAR(255) NOT NULL`
— BCrypt hashes are 60 characters. `VARCHAR(255)` gives headroom for any algorithm. The column is named `password_hash`, not `password` — communicating clearly that raw passwords are never stored.

`role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER'`
— Two values in practice: `CUSTOMER` and `BANK_ADMIN`. Using `VARCHAR` instead of a PostgreSQL `ENUM` keeps it flexible — adding a new role requires zero schema changes. The `DEFAULT 'CUSTOMER'` means every new user is least-privilege by default.

`created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
— `TIMESTAMPTZ` = timestamp *with time zone*. Always use this over bare `TIMESTAMP` — it stores UTC and converts on read. `DEFAULT NOW()` is set by the database, not by application code, so it's immune to clock skew between app servers.

**Seed data:**
```sql
INSERT INTO users (email, password_hash, role) VALUES
  ('admin@bank.com',    '$2b$10$j6NhqxHWYlRmz...', 'BANK_ADMIN'),
  ('alice@example.com', '$2b$10$d3FCdNrbGNv...', 'CUSTOMER'),
  ('bob@example.com',   '$2b$10$d3FCdNrbGNv...', 'CUSTOMER')
ON CONFLICT (email) DO NOTHING;
```

`$2b$10$...` is a BCrypt hash. `$2b$` = BCrypt algorithm, `$10$` = 10 cost rounds (2^10 = 1024 iterations). The plaintext passwords are `admin123` / `customer123`.

`ON CONFLICT (email) DO NOTHING` makes this insert **idempotent** — if you run `docker-compose down && docker-compose up`, Flyway re-applies V1 on a fresh database, and the seed insert runs cleanly without duplicating rows.

---

### File 2: `V2__create_accounts.sql`

```sql
CREATE TABLE IF NOT EXISTS accounts (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(20)    NOT NULL UNIQUE,
    owner_id       UUID           NOT NULL REFERENCES users(id),
    balance        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency       VARCHAR(3)     NOT NULL DEFAULT 'USD',
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_owner_id ON accounts(owner_id);
```

**The critical column — `balance NUMERIC(19, 4)`:**

`NUMERIC(19, 4)` means: up to 19 significant digits total, 4 digits after the decimal point. **Never use `FLOAT` or `DOUBLE` for money.** Floating-point types are base-2 approximations. `0.1 + 0.2` in a float is `0.30000000000000004`. In a financial system, a $0.001 rounding error on every transaction compounds into real money. `NUMERIC` is exact decimal arithmetic — `0.10 + 0.20 = 0.30` guaranteed.

`owner_id UUID NOT NULL REFERENCES users(id)`
— Foreign key to the `users` table. PostgreSQL enforces referential integrity: you cannot insert an account with an `owner_id` that doesn't exist in `users`. You cannot delete a user who owns accounts. This constraint lives in the database, not the application — it holds even if someone runs a raw SQL script directly.

`status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`
— Three states in practice: `ACTIVE`, `FROZEN`, `CLOSED`. The `TransactionService` checks `"ACTIVE".equals(fromAccount.getStatus())` before allowing any transfer.

**The index:**
```sql
CREATE INDEX idx_accounts_owner_id ON accounts(owner_id);
```
`AccountRepository.findByOwnerId(UUID ownerId)` generates `WHERE owner_id = ?`. Without this index, PostgreSQL does a full table scan on every call. With it, the lookup is O(log n). Since the `accounts` table will be queried by `owner_id` on every dashboard load, this index pays for itself immediately.

**Seed data — `INSERT ... SELECT` pattern:**
```sql
INSERT INTO accounts (account_number, owner_id, balance, currency, status)
SELECT 'ACC-ALICE-001', id, 50000.00, 'USD', 'ACTIVE'
FROM users WHERE email = 'alice@example.com'
ON CONFLICT (account_number) DO NOTHING;
```

This uses `INSERT ... SELECT` instead of `INSERT ... VALUES`. Why? Because Alice's `id` (UUID) was generated randomly by `gen_random_uuid()` in V1 — we don't know it at script-writing time. This query finds Alice's UUID dynamically and uses it as `owner_id`. This is the correct way to reference seed data across migrations without hardcoding UUIDs.

Initial balances: **Alice = $50,000**, **Bob = $20,000**.

---

### File 3: `V3__create_transactions.sql`

```sql
CREATE TABLE IF NOT EXISTS transactions (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    from_account_id  UUID           NOT NULL REFERENCES accounts(id),
    to_account_id    UUID           NOT NULL REFERENCES accounts(id),
    amount           NUMERIC(19, 4) NOT NULL,
    currency         VARCHAR(3)     NOT NULL DEFAULT 'USD',
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDING_FRAUD_CHECK',
    idempotency_key  VARCHAR(255)   NOT NULL UNIQUE,
    description      TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
```

`from_account_id` and `to_account_id` both `REFERENCES accounts(id)` — two foreign keys into the same table. A transaction is a directed edge between two accounts.

`status VARCHAR(30) NOT NULL DEFAULT 'PENDING_FRAUD_CHECK'`
— Every transaction starts frozen, waiting for fraud clearance. The six possible values (`PENDING_FRAUD_CHECK`, `PROCESSING`, `COMPLETED`, `FAILED`, `FRAUD_REJECTED`, `REVERSED`) are enforced by the `TransactionStatus` enum in Java. We'll see that state machine in Phase 4.

`idempotency_key VARCHAR(255) NOT NULL UNIQUE`
— The client generates a UUID before sending the request. The `UNIQUE` constraint at the database level is the final guard against double-processing. If a network timeout causes the client to retry, the second insert hits the unique constraint and the service returns the existing result instead of creating a duplicate transfer.

**Four indexes:**
```sql
CREATE INDEX idx_transactions_from_account ON transactions(from_account_id);
CREATE INDEX idx_transactions_to_account   ON transactions(to_account_id);
CREATE INDEX idx_transactions_status       ON transactions(status);
CREATE INDEX idx_transactions_idempotency  ON transactions(idempotency_key);
```

| Index | Query it serves |
|---|---|
| `from_account` | `findByFromAccountIdOrToAccountId(...)` — transaction history page |
| `to_account` | same query, the other half of the OR |
| `status` | admin queries filtering by `PENDING_FRAUD_CHECK`, `FAILED`, etc. |
| `idempotency` | `findByIdempotencyKey(key)` on every new transfer request |

The `idempotency_key` index is the most latency-sensitive — it runs on every single transfer initiation.

---

### Full Schema Relationship

```
users (id PK)
  └─── accounts (id PK, owner_id FK → users.id)
         └─── transactions (id PK,
                             from_account_id FK → accounts.id,
                             to_account_id   FK → accounts.id)
```

Three tables. Two levels of foreign key depth. PostgreSQL enforces every arrow at write time.

---

### What `flyway_schema_history` Looks Like After First Boot

```
version | description         | script                       | success
--------|---------------------|------------------------------|--------
1       | create users        | V1__create_users.sql         | true
2       | create accounts     | V2__create_accounts.sql      | true
3       | create transactions | V3__create_transactions.sql  | true
```

If you edit `V1__create_users.sql` after it's been applied, Flyway computes a new checksum, finds it doesn't match the stored one, and **refuses to start the application**. The only safe way to change the schema after deployment is to add a new `V4__...sql` file.

---

### Connection to Phase 1

In `application.yml` you saw `flyway.locations: classpath:db/migration` and `jpa.hibernate.ddl-auto: validate`. These two settings work together — Flyway *builds* the schema, Hibernate *verifies* it matches the entities. In Phase 3 you'll see how each SQL column maps exactly to a Java field.

---

### Phase 2 Challenge Question

V2 seeds Alice's account with `balance = 50000.00`. After a few transfers, Alice's balance is $47,300. You run `docker-compose down -v` (the `-v` flag removes volumes, wiping the database). You run `docker-compose up` again. What is Alice's balance now?

> **Answer:** `$50,000.00`. The `-v` flag destroys the `postgres_data` volume, so the entire database is gone. Flyway re-runs all three scripts on the fresh database. V2 seeds Alice's account at `50000.00` again. All transfer history is also gone — the `transactions` table is recreated empty. This is why `-v` is dangerous and you'd never run it in production.
