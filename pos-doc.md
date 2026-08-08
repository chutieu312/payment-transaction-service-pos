# PAT Financial Operations Service (PAT-FOS) — Project Documentation

> **Purpose of this file:** Single source of truth for the entire project. New team members start here. When adding a new feature or service, update the relevant section and add it to the architecture diagram.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture Diagram](#2-architecture-diagram)
3. [Service Map](#3-service-map)
4. [Frontend](#4-frontend)
5. [Transaction Service (Backend)](#5-transaction-service-backend)
6. [Fraud Detection Service](#6-fraud-detection-service)
7. [Databases](#7-databases)
8. [Message Queue — Kafka](#8-message-queue--kafka)
9. [Caching — Redis](#9-caching--redis)
10. [CI/CD Pipeline](#10-cicd-pipeline)
11. [Docker & Local Development](#11-docker--local-development)
12. [Security Model](#12-security-model)
13. [API Reference](#13-api-reference)
14. [Kafka Topics & Events](#14-kafka-topics--events)
15. [Fraud Rules](#15-fraud-rules)
16. [Database Schema](#16-database-schema)
17. [Environment Variables](#17-environment-variables)
18. [Running the Project](#18-running-the-project)
19. [Feature & Change Log](#19-feature--change-log)

---

## 1. Project Overview

PAT-FOS is a **cloud-native, event-driven payment microservices system** built to simulate inter-team budget transfer operations within an enterprise platform.

| Attribute | Value |
|---|---|
| Primary language (backend) | Java 21 |
| Secondary language (fraud) | Python 3.12 |
| Frontend | React 18 + TypeScript + Tailwind CSS |
| Transport | REST (sync) + Kafka (async) |
| Databases | PostgreSQL 16, MongoDB 7, Redis 7 |
| Container runtime | Docker + Docker Compose |
| CI/CD | GitHub Actions → GHCR |

**Core business flows:**
- Users authenticate via JWT and manage budget accounts
- A team lead initiates a fund transfer (async — returns `202 Accepted` immediately)
- Kafka carries the transfer event to the Fraud Detection Service
- Fraud service evaluates rules, publishes assessment back via Kafka
- Transaction service consumes the assessment and marks the transfer `COMPLETED` or `FRAUD_REJECTED`
- Every state transition is stored as an audit event in MongoDB

---

## 2. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USER BROWSER                                   │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │  HTTPS  (port 3000)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND  (React + Vite)                            │
│   /login  /  /transactions  /admin                                          │
│   Nginx serves static build · JWT stored in localStorage                    │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │  REST /api/*  (port 8080)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                   TRANSACTION SERVICE  (Java 21 · Spring Boot 3.3)          │
│                                                                             │
│   Controllers:  /api/auth  /api/accounts  /api/transfers                   │
│   Security:     JWT filter → @PreAuthorize (CUSTOMER | BANK_ADMIN)         │
│   Business:     TransactionService · AccountService · AuditService         │
│                                                                             │
│   ┌──────────────┐  ┌─────────────┐  ┌────────────────────────────────┐   │
│   │  PostgreSQL   │  │    Redis    │  │           MongoDB              │   │
│   │  (ledger)     │  │  (cache)   │  │  (audit events / tx history)   │   │
│   │  users        │  │  balances  │  │  payments_db.transaction_events│   │
│   │  accounts     │  │  TTL: 60s  │  │                                │   │
│   │  transactions │  └─────────────┘  └────────────────────────────────┘   │
│   └──────────────┘                                                          │
│                                                                             │
│   Kafka PRODUCER ──► transaction.initiated                                  │
│   Kafka CONSUMER ◄── fraud.assessment                                       │
│   Kafka PRODUCER ──► transaction.completed                                  │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │  Kafka topics
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
         transaction.initiated            fraud.assessment
         transaction.completed
                    │
                    ▼
┌───────────────────────────────────────────────────────────┐
│            FRAUD DETECTION SERVICE  (Python · FastAPI)    │
│                                                           │
│   Kafka CONSUMER ◄── transaction.initiated                │
│   Evaluates rules:  AMOUNT_THRESHOLD                      │
│                     VELOCITY_CHECK                        │
│                     BLOCKED_ACCOUNT                       │
│   Kafka PRODUCER ──► fraud.assessment                     │
│                                                           │
│   REST API:  /fraud/rules  /fraud/assessments  /alerts    │
│                                                           │
│   ┌──────────────────────────────────────────────────┐    │
│   │                  MongoDB  (fraud_db)              │    │
│   │  fraud_rules  ·  fraud_assessments               │    │
│   └──────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────┘

────────────────────── INFRASTRUCTURE ──────────────────────

  Zookeeper ◄──► Kafka          (ports 2181, 9092)
  PostgreSQL                    (port 5432)
  MongoDB                       (port 27017)
  Redis                         (port 6379)
  Kafka UI     [Provectus]      (port 8082)
  Adminer      [Postgres UI]    (port 8888)
  Mongo Express                 (port 8081)

────────────────────── CI / CD ─────────────────────────────

  GitHub Actions
    │
    ├── Java Unit Tests   (Maven + JUnit)
    ├── Python Unit Tests (pytest + ruff lint)
    ├── Frontend Build    (npm ci + tsc + vite build)
    ├── Integration Tests (Testcontainers)
    └── Build & Push Docker Images → ghcr.io  (main only)
```

---

## 3. Service Map

| Service | Language | Port | Responsibility |
|---|---|---|---|
| `frontend` | React / TypeScript | 3000 | UI — login, dashboard, transfers, admin panel |
| `transaction-service` | Java 21 / Spring Boot | 8080 | Core API — auth, accounts, transfers, Kafka orchestration |
| `fraud-service` | Python 3.12 / FastAPI | 8090 | Fraud rule evaluation via Kafka events |
| `postgres` | PostgreSQL 16 | 5432 | Transactional ledger (users, accounts, transactions) |
| `mongodb` | MongoDB 7 | 27017 | Audit events (payments_db) + fraud data (fraud_db) |
| `redis` | Redis 7 | 6379 | Balance cache (TTL 60s) |
| `kafka` | Confluent Kafka | 9092 | Async event bus between services |
| `zookeeper` | Zookeeper | 2181 | Kafka broker coordination |
| `kafka-ui` | Provectus Kafka UI | 8082 | Kafka topic browser (dev only) |
| `adminer` | Adminer | 8888 | PostgreSQL browser (dev only) |
| `mongo-express` | Mongo Express | 8081 | MongoDB browser (dev only) |

---

## 4. Frontend

**Stack:** React 18 · TypeScript · Vite · Tailwind CSS · Axios · React Router v6

**Pages:**

| Route | Component | Access |
|---|---|---|
| `/login` | `LoginPage.tsx` | Public |
| `/` | `AccountDashboard.tsx` | Authenticated |
| `/transactions` | `TransactionHistory.tsx` | Authenticated |
| `/admin` | `AdminPanel.tsx` | Authenticated (admin UI) |

**Key files:**

| File | Purpose |
|---|---|
| `src/api/transactionApi.ts` | All HTTP calls to the backend + JWT helpers |
| `src/App.tsx` | Route definitions + `RequireAuth` guard |
| `src/components/` | Reusable UI: `BalanceCard`, `TransferForm`, `TransactionRow`, `StatusBadge` |

**Auth flow:**
1. User logs in → backend returns JWT
2. JWT saved to `localStorage`
3. Axios interceptor attaches `Authorization: Bearer <token>` on every request
4. `RequireAuth` wrapper redirects to `/login` if no token

**Build:**
- Dev: `npm run dev` (Vite HMR on port 5173)
- Production: `npm run build` → Nginx serves `/dist` on port 3000

---

## 5. Transaction Service (Backend)

**Stack:** Java 21 · Spring Boot 3.3 · Spring Security · Spring Data JPA · Spring Kafka · Flyway · Lombok · Swagger/OpenAPI

**Package structure:**
```
com.fpt.payments/
├── config/          SecurityConfig, RedisConfig, KafkaConfig, SwaggerConfig
├── controller/      TransactionController, AccountController, AuthController
├── dto/             TransferRequest, TransferResponse, AccountResponse, BalanceResponse
├── entity/          User, Account, Transaction  (JPA entities)
├── enums/           TransactionStatus
├── exception/       GlobalExceptionHandler (RFC 9457 ProblemDetail)
├── kafka/           TransactionEventProducer, FraudAssessmentConsumer
├── repository/      UserRepository, AccountRepository, TransactionRepository
├── security/        JwtFilter, JwtUtil
└── service/         TransactionService, AccountService, AuditService
```

**Transaction lifecycle:**

```
POST /api/transfers/{fromAccountId}
  │
  ├─ 1. Validate request (Bean Validation)
  ├─ 2. Check idempotency key (duplicate detection)
  ├─ 3. Debit sender + Credit receiver (@Transactional — atomic)
  ├─ 4. Save transaction with status=PENDING_FRAUD_CHECK
  ├─ 5. Publish → transaction.initiated (Kafka)
  └─ 6. Return 202 Accepted immediately

  [Async — Kafka consumer]
  ├─ Consume ← fraud.assessment
  ├─ Update transaction status → COMPLETED or FRAUD_REJECTED
  ├─ If REJECTED → reverse the balance debit/credit
  └─ Publish → transaction.completed (Kafka)
```

**Transaction statuses:**

| Status | Meaning |
|---|---|
| `PENDING_FRAUD_CHECK` | Submitted, waiting for fraud evaluation |
| `PROCESSING` | Fraud service is evaluating |
| `COMPLETED` | Approved and settled |
| `FRAUD_REJECTED` | Rejected by fraud engine, balances reversed |
| `REVERSED` | Admin-reversed after completion |

**Key config (`application.yml`):**
- JWT expiry: 24 hours
- Balance cache TTL: 60 seconds
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`

---

## 6. Fraud Detection Service

**Stack:** Python 3.12 · FastAPI · aiokafka · Motor (async MongoDB) · Pydantic

**Startup sequence:**
1. Seeds default fraud rules into MongoDB (if not already present)
2. Launches background Kafka consumer task (`start_consumer()`)
3. FastAPI serves REST endpoints for rule management

**Fraud evaluation pipeline:**

```
Kafka message arrives on transaction.initiated
  │
  ├─ Idempotency check (already assessed this tx_id?)
  ├─ Rule 1: AMOUNT_THRESHOLD  — amount > $10,000 → +50 risk score
  ├─ Rule 2: VELOCITY_CHECK    — >5 tx from same account in window → +40 risk score
  ├─ Rule 3: BLOCKED_ACCOUNT   — account on blocklist → +100 risk score
  │
  ├─ Total risk score ≥ threshold → decision = REJECTED
  ├─ Total risk score < threshold → decision = APPROVED
  │
  ├─ Save FraudAssessment to MongoDB (fraud_db.fraud_assessments)
  └─ Publish → fraud.assessment (Kafka)
```

**REST endpoints (port 8090):**

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Health check |
| GET | `/fraud/rules/` | List all fraud rules |
| POST | `/fraud/rules/` | Create a new rule |
| PUT | `/fraud/rules/{id}` | Update a rule |
| GET | `/fraud/assessments/` | List all assessments |
| GET | `/fraud/alerts/` | List rejected transactions |

---

## 7. Databases

### PostgreSQL 16 — Transactional Ledger

**Database:** `payments_db`  
**Managed by:** Flyway (schema migrations in `transaction-service/src/main/resources/db/migration/`)

| Migration | File | What it creates |
|---|---|---|
| V1 | `V1__create_users.sql` | `users` table + seed data (admin, alice, bob) |
| V2 | `V2__create_accounts.sql` | `accounts` table + seed accounts |
| V3 | `V3__create_transactions.sql` | `transactions` table + indexes |

**Seed credentials:**

| Email | Password | Role |
|---|---|---|
| `admin@bank.com` | `admin123` | `BANK_ADMIN` |
| `alice@example.com` | `customer123` | `CUSTOMER` |
| `bob@example.com` | `customer123` | `CUSTOMER` |

### MongoDB 7 — Document Store

Two logical databases:

| Database | Collection | Purpose |
|---|---|---|
| `payments_db` | `transaction_events` | Immutable audit log — every transaction state change |
| `fraud_db` | `fraud_rules` | Configurable fraud detection rules |
| `fraud_db` | `fraud_assessments` | One assessment document per transaction |

### Redis 7 — Cache

| Key pattern | Value | TTL |
|---|---|---|
| `balances::<accountId>` | JSON balance response | 60 seconds |

Cache is **invalidated immediately** when a transfer affects the account. Subsequent reads within 60s are served from Redis (sub-millisecond).

---

## 8. Message Queue — Kafka

**Broker:** Confluent Kafka 7.6.1  
**Coordinator:** Zookeeper 7.6.1

### Topics

| Topic | Producer | Consumer | Payload |
|---|---|---|---|
| `transaction.initiated` | transaction-service | fraud-service | `{transactionId, fromAccountId, toAccountId, amount, currency, correlationId}` |
| `fraud.assessment` | fraud-service | transaction-service | `{transactionId, decision, riskScore, reasons, timestamp}` |
| `transaction.completed` | transaction-service | — (audit/future consumers) | Final transaction state |

### Consumer Groups

| Group ID | Service | Subscribes to |
|---|---|---|
| `fraud-service` | fraud-service (Python) | `transaction.initiated` |
| `transaction-service` | transaction-service (Java) | `fraud.assessment` |

**Design principle:** The two services **never call each other directly**. All communication is via Kafka — this decouples them so either can be deployed, scaled, or restarted independently.

---

## 9. Caching — Redis

**Pattern:** Cache-aside with TTL

```
GET /api/accounts/{id}/balance
  │
  ├─ Check Redis key: balances::<id>
  │     HIT  → return cached value (< 1ms)
  │     MISS → query PostgreSQL → store in Redis with TTL=60s → return
  │
POST /api/transfers  (on success)
  └─ Evict: balances::<fromAccountId>
  └─ Evict: balances::<toAccountId>
```

**Spring annotation:** `@Cacheable(value = "balances", key = "#accountId")` on `AccountService.getBalance()`

---

## 10. CI/CD Pipeline

**Platform:** GitHub Actions  
**Registry:** GitHub Container Registry (`ghcr.io`)

```
PR opened / push to main or develop
  │
  ├── [parallel] Java Unit Tests
  │     Maven → JUnit 5 + Mockito
  │     Uploads surefire report as artifact
  │
  ├── [parallel] Python Unit Tests
  │     pip install → ruff lint → pytest
  │
  ├── [parallel] Frontend Build
  │     npm ci → tsc --noEmit → vite build
  │
  └── [after all 3 pass] Integration Tests
        Maven Testcontainers (spins up real Postgres/Redis/Kafka in CI)
        │
        └── [after integration, main branch only] Build & Push Docker Images
              transaction-service → ghcr.io/.../transaction-service:<SHA>
              fraud-service       → ghcr.io/.../fraud-service:<SHA>
              frontend            → ghcr.io/.../frontend:<SHA>
```

**Branch protection on `main`:**
- All 5 status checks must pass
- At least 1 approving review required
- Direct pushes blocked — must use PR

---

## 11. Docker & Local Development

**Compose file:** `docker-compose.yml`  
**Total containers:** 11

### Startup order (via `depends_on` health checks)

```
zookeeper (healthy)
  └── kafka (healthy)
        └── transaction-service
        └── fraud-service
              └── frontend

postgres (healthy) ──► transaction-service
redis    (healthy) ──► transaction-service
mongodb  (healthy) ──► transaction-service
                   ──► fraud-service
```

### Convenience scripts

| Script | Command | What it does |
|---|---|---|
| Start everything | `./run-app.sh` | Build images + start all 11 services + wait for health |
| Stop everything | `./stop-app.sh` | `docker compose down` (keeps volumes) |
| Wipe all data | `docker compose down -v` | Removes volumes (fresh start) |

> **Note:** `run-app.sh` auto-recovers from the Kafka/Zookeeper stale node issue that occurs when containers are recreated.

### Ports at a glance

| URL | Service |
|---|---|
| http://localhost:3000 | Frontend (React app) |
| http://localhost:8080 | Transaction Service API |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8090 | Fraud Detection API |
| http://localhost:8082 | Kafka UI |
| http://localhost:8888 | Adminer (Postgres UI) |
| http://localhost:8081 | Mongo Express |

---

## 12. Security Model

**Authentication:** Stateless JWT (HS256)  
**Token expiry:** 24 hours  
**Storage:** `localStorage` on frontend

```
Login → POST /api/auth/login → { token: "eyJ..." }
  │
  └── Every subsequent request:
        Authorization: Bearer eyJ...
          │
          └── JwtFilter validates signature + expiry
                └── Sets SecurityContext with userId + role
                      └── @PreAuthorize checks role
```

**Roles:**

| Role | Can do |
|---|---|
| `CUSTOMER` | View own account, view own balance, initiate transfers |
| `BANK_ADMIN` | Everything + list all transfers + reverse transactions |

**Security layers:**
1. **Transport:** JWT signature validation on every request (`JwtFilter`)
2. **Method-level:** `@PreAuthorize("hasRole('BANK_ADMIN')")` on admin endpoints
3. **URL-level:** All routes except `/api/auth/**` and `/actuator/health` require authentication
4. **Password storage:** BCrypt hashing
5. **Error responses:** RFC 9457 `ProblemDetail` — no internal stack traces exposed

---

## 13. API Reference

### Auth (`/api/auth`)

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/auth/login` | `{email, password}` | `{token}` |
| POST | `/api/auth/register` | `{email, password, role}` | `{token}` |

### Accounts (`/api/accounts`) — Requires JWT

| Method | Path | Response |
|---|---|---|
| GET | `/api/accounts/{id}` | Account details |
| GET | `/api/accounts/{id}/balance` | Balance (Redis cached) |
| GET | `/api/accounts/{id}/transactions` | Transaction history |

### Transfers (`/api/transfers`) — Requires JWT

| Method | Path | Roles | Response |
|---|---|---|---|
| POST | `/api/transfers/{fromAccountId}` | CUSTOMER | `202 Accepted` + transfer record |
| GET | `/api/transfers/{id}` | Any | Transfer status (poll this) |
| GET | `/api/transfers` | BANK_ADMIN | All transfers |
| POST | `/api/transfers/{id}/reverse` | BANK_ADMIN | Reversed transfer |

**Transfer request body:**
```json
{
  "toAccountId": "uuid",
  "amount": 500.00,
  "currency": "USD",
  "idempotencyKey": "unique-key-per-request",
  "description": "optional note"
}
```

---

## 14. Kafka Topics & Events

### `transaction.initiated` (transaction-service → fraud-service)

```json
{
  "transactionId": "uuid",
  "correlationId": "uuid",
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": 500.00,
  "currency": "USD",
  "timestamp": "2026-08-05T10:00:00Z"
}
```

### `fraud.assessment` (fraud-service → transaction-service)

```json
{
  "transactionId": "uuid",
  "correlationId": "uuid",
  "decision": "APPROVED | REJECTED",
  "riskScore": 50,
  "reasons": ["AMOUNT_THRESHOLD_EXCEEDED"],
  "timestamp": "2026-08-05T10:00:01Z"
}
```

---

## 15. Fraud Rules

Rules are stored in MongoDB (`fraud_db.fraud_rules`) and seeded on startup. They can be updated at runtime via the fraud service REST API without redeployment.

| Rule | Trigger | Risk Score Added |
|---|---|---|
| `AMOUNT_THRESHOLD` | Transfer > $10,000 | +50 |
| `VELOCITY_CHECK` | > 5 transfers from same account in window | +40 |
| `BLOCKED_ACCOUNT` | Account on blocklist | +100 |

**Decision threshold:** Total risk score ≥ 70 → `REJECTED`

**To add a new rule:**
```bash
POST http://localhost:8090/fraud/rules/
{
  "name": "NEW_RULE",
  "description": "...",
  "enabled": true,
  "risk_score_contribution": 30,
  "threshold_value": 5000.0
}
```

---

## 16. Database Schema

### PostgreSQL

```
users
  id            UUID  PK
  email         VARCHAR(255)  UNIQUE
  password_hash VARCHAR(255)
  role          VARCHAR(50)   [CUSTOMER | BANK_ADMIN]
  created_at    TIMESTAMPTZ

accounts
  id             UUID  PK
  account_number VARCHAR(20)   UNIQUE
  owner_id       UUID  FK → users.id
  balance        NUMERIC(19,4)
  currency       VARCHAR(3)    [USD]
  status         VARCHAR(20)   [ACTIVE | FROZEN]
  created_at     TIMESTAMPTZ
  updated_at     TIMESTAMPTZ

transactions
  id              UUID  PK
  from_account_id UUID  FK → accounts.id
  to_account_id   UUID  FK → accounts.id
  amount          NUMERIC(19,4)
  currency        VARCHAR(3)
  status          VARCHAR(30)   [PENDING_FRAUD_CHECK | COMPLETED | FRAUD_REJECTED | REVERSED]
  idempotency_key VARCHAR(255)  UNIQUE
  description     TEXT
  created_at      TIMESTAMPTZ
  updated_at      TIMESTAMPTZ
```

### MongoDB — `fraud_db`

```
fraud_rules
  _id                    ObjectId
  name                   String
  description            String
  enabled                Boolean
  risk_score_contribution Number
  threshold_value        Number | null

fraud_assessments
  _id            ObjectId
  transaction_id String
  from_account_id String
  to_account_id   String
  amount          Number
  currency        String
  risk_score      Number
  decision        String  [APPROVED | REJECTED]
  reasons         Array<String>
  evaluated_at    DateTime
```

### MongoDB — `payments_db`

```
transaction_events
  _id            ObjectId
  transaction_id String
  event_type     String  [INITIATED | FRAUD_APPROVED | FRAUD_REJECTED | COMPLETED | REVERSED]
  payload        Object
  timestamp      DateTime
```

---

## 17. Environment Variables

### transaction-service

| Variable | Default | Description |
|---|---|---|
| `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_DB` | `payments_db` | Database name |
| `POSTGRES_USER` | `payments_user` | DB username |
| `POSTGRES_PASSWORD` | `payments_pass` | DB password |
| `MONGO_HOST` | `localhost` | MongoDB host |
| `MONGO_DB` | `payments_db` | MongoDB database |
| `REDIS_HOST` | `localhost` | Redis host |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker |
| `JWT_SECRET` | (base64 default) | JWT signing secret |

### fraud-service

| Variable | Default | Description |
|---|---|---|
| `MONGO_URI` | `mongodb://localhost:27017` | MongoDB connection |
| `MONGO_DB` | `fraud_db` | MongoDB database |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker |

---

## 18. Running the Project

### Prerequisites

- Docker Desktop running
- Bash (WSL on Windows or native Linux/macOS)

### Start

```bash
./run-app.sh
```

Builds all Docker images (first run: ~5 min), starts all 11 containers, waits for health checks, prints all service URLs.

### Stop (keep data)

```bash
./stop-app.sh
```

### Stop + wipe all data

```bash
docker compose down -v
```

### Run tests locally

```bash
# Java tests
cd transaction-service && mvn test

# Python tests
cd fraud-service && pytest -v

# Frontend type check
cd frontend && npx tsc --noEmit
```

---

## 19. Feature & Change Log

> **Instructions for new team members:** When you add a feature, add an entry here. When a new member joins, they read this section from bottom to top to understand what has changed over time.

| Date | Author | Change |
|---|---|---|
| 2026-06 | Can Nguyen | Initial project — transaction-service, fraud-service, frontend, PostgreSQL, MongoDB, Redis, Kafka |
| 2026-08-05 | Can Nguyen | Added `run-app.sh` — one-command full stack startup with Kafka/Zookeeper auto-recovery |
| 2026-08-05 | Can Nguyen | Added `stop-app.sh` — one-command full stack shutdown |
| 2026-08-05 | Can Nguyen | Fixed CI pipeline — ruff lint, pytest pythonpath, JUnit WebMvcTest method security |
| 2026-08-08 | Can Nguyen | Created `pos-doc.md` — full project documentation |

---

*Last updated: 2026-08-08 · Maintained by: Can Nguyen*
