# PAT Financial Operations Service (PAT-FOS)

> **Part of the Product Agility Tools (PAT) Platform** — JP Morgan Chase  
> A microservice within PAT that manages team budget allocations, processes inter-team budget transfers, and uses an event-driven anomaly detection engine to flag unauthorized or unusual spending patterns across 2,000+ internal teams.

A full-stack, event-driven system built with **Java 21 / Spring Boot 3**, **Python 3.12 / FastAPI**, **React 18 / TypeScript**, **Apache Kafka**, **PostgreSQL**, **MongoDB**, and **Redis** — deployed via Docker Compose.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Getting Started](#getting-started)
3. [Service Map & Credentials](#service-map--credentials)
4. [Demo Accounts](#demo-accounts)
5. [Database Access](#database-access)
6. [Using the Frontend](#using-the-frontend)
7. [Using the REST API](#using-the-rest-api)
8. [How the Transfer Flow Works](#how-the-transfer-flow-works)
9. [Fraud Detection Rules](#fraud-detection-rules)
10. [Running Tests](#running-tests)
11. [Stopping & Resetting](#stopping--resetting)
12. [Architecture](#architecture)
13. [JD Skills Demonstrated](#jd-skills-demonstrated)

---

## Prerequisites

| Tool | Minimum version | Check |
|---|---|---|
| Docker Desktop | 24+ | `docker --version` |
| Docker Compose | v2.20+ | `docker compose version` |

No local Java, Python, or Node.js installation is required — everything runs inside Docker.

---

## Getting Started

```bash
# 1. Clone the repository
git clone <your-repo-url>
cd payment-transaction-service

# 2. Create your local environment file
cp .env.example .env
# The defaults in .env work out of the box — no edits needed for local dev.

# 3. Build all images and start all 11 services
docker compose up --build

# Wait for this line in the logs before testing:
#   transaction-service-1  | Started PaymentTransactionApplication in X.XXX seconds
```

> **First run takes ~3–5 minutes** — Maven downloads ~200 MB of dependencies into the Docker layer cache. Subsequent starts take under 30 seconds.

### Start / Stop (after first build)

```bash
docker compose up -d          # start in background
docker compose down           # stop (keeps data volumes)
docker compose down -v        # stop AND wipe all data (clean slate)
docker compose logs -f        # stream all logs
docker compose logs -f transaction-service   # stream one service
```

---

## Service Map & Credentials

All services run on `localhost`. Open these URLs in your browser after `docker compose up`.

| Service | URL | Credentials |
|---|---|---|
| **Frontend** (React SPA) | http://localhost:3000 | see [Demo Accounts](#demo-accounts) |
| **Transaction Service** (Spring Boot API) | http://localhost:8080 | JWT — see below |
| **Swagger UI** (interactive API docs) | http://localhost:8080/swagger-ui.html | none |
| **Fraud Service** (FastAPI) | http://localhost:8090 | none |
| **FastAPI Docs** (interactive API docs) | http://localhost:8090/docs | none |
| **Kafka UI** (topic browser) | http://localhost:8082 | none |
| **Adminer** (PostgreSQL web UI) | http://localhost:8888 | see [Database Access](#database-access) |
| **Mongo Express** (MongoDB web UI) | http://localhost:8081 | none |

---

## Demo Accounts

These users are seeded automatically by Flyway on first startup.

| Role | Email | Password | Can do |
|---|---|---|---|
| `BANK_ADMIN` | `admin@bank.com` | `admin123` | View all transactions, reverse completed ones |
| `CUSTOMER` | `alice@example.com` | `customer123` | Transfer from ACC-ALICE-001 (balance: $50,000) |
| `CUSTOMER` | `bob@example.com` | `customer123` | Transfer from ACC-BOB-001 (balance: $20,000) |

> **Frontend login:** go to http://localhost:3000, enter any of the email/password pairs above.
>
> **API login:** returns a JWT — paste it into Swagger UI's "Authorize" button (bearer token).

```bash
# Get a JWT token for Alice
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"customer123"}' | python3 -m json.tool
```

---

## Database Access

### PostgreSQL — Adminer (http://localhost:8888)

| Field | Value |
|---|---|
| System | PostgreSQL |
| Server | `postgres` |
| Username | `payments` |
| Password | `payments` |
| Database | `payments_db` |

Key tables: `users`, `accounts`, `transactions`

```bash
# Or connect directly via psql
docker exec -it payment-transaction-service-postgres-1 \
  psql -U payments -d payments_db

# Useful queries
SELECT email, role FROM users;
SELECT account_number, balance, currency FROM accounts;
SELECT id, status, amount FROM transactions ORDER BY created_at DESC LIMIT 10;
```

### MongoDB — Mongo Express (http://localhost:8081)

No login required. Two databases:

| Database | What's in it |
|---|---|
| `payments_db` | `transaction_events` — append-only audit log of every status change |
| `fraud_db` | `fraud_assessments` — every fraud decision, `fraud_rules` — active rule set |

```bash
# Or connect directly via mongosh
docker exec -it payment-transaction-service-mongodb-1 mongosh

# Useful queries
use fraud_db
db.fraud_assessments.find().sort({evaluated_at:-1}).limit(5).pretty()
db.fraud_rules.find().pretty()

use payments_db
db.transaction_events.find().sort({timestamp:-1}).limit(5).pretty()
```

### Redis

Redis holds two types of cached data:

| Key pattern | Content | TTL |
|---|---|---|
| `balances::<accountId>` | Cached balance response | 60 seconds |
| Idempotency keys | Stored in PostgreSQL `transactions.idempotency_key` | permanent |

```bash
# Connect to Redis CLI
docker exec -it payment-transaction-service-redis-1 redis-cli
KEYS *
```

---

## Using the Frontend

1. Open http://localhost:3000
2. Log in with any account from the [Demo Accounts](#demo-accounts) table
3. Set `VITE_ACCOUNT_ID` in your `.env` to your account's UUID, then rebuild frontend — **or** skip the balance card and go straight to transfers

> **Tip:** Get your account UUID from Adminer or with:
> ```bash
> docker exec payment-transaction-service-postgres-1 \
>   psql -U payments -d payments_db \
>   -c "SELECT a.id, a.account_number, u.email FROM accounts a JOIN users u ON a.owner_id=u.id;"
> ```

**Pages:**
- `/` — Account dashboard + transfer form
- `/transactions` — Your transaction history with status badges
- `/admin` — All transactions + reverse button (requires `BANK_ADMIN` JWT)

---

## Using the REST API

The easiest way is **Swagger UI** at http://localhost:8080/swagger-ui.html:

1. Click **POST /api/auth/login** → Try it out → enter credentials → Execute
2. Copy the `token` from the response
3. Click the **Authorize 🔒** button at the top → paste the token → Authorize
4. All endpoints are now authenticated

### Key endpoints

```bash
export TOKEN="<paste your JWT here>"

# Get account balance
curl -s http://localhost:8080/api/accounts/<accountId>/balance \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# Initiate a transfer (returns 202 Accepted immediately)
curl -s -X POST http://localhost:8080/api/transfers/<fromAccountId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "toAccountId": "<bobAccountId>",
    "amount": 500,
    "currency": "USD",
    "idempotencyKey": "any-unique-string-001",
    "description": "test payment"
  }' | python3 -m json.tool

# Check transaction status
curl -s http://localhost:8080/api/transfers/<transactionId> \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# Check fraud assessment for a transaction
curl -s http://localhost:8090/fraud/assessments/<transactionId> | python3 -m json.tool

# View all fraud alerts (REJECTED decisions)
curl -s http://localhost:8090/fraud/alerts/ | python3 -m json.tool

# Reverse a COMPLETED transaction (BANK_ADMIN only)
curl -s -X POST http://localhost:8080/api/transfers/<transactionId>/reverse \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

## How the Transfer Flow Works

```
POST /api/transfers/{fromAccountId}
      │
      ├─ 1. Check idempotency key → return existing if duplicate
      ├─ 2. Validate account is ACTIVE + sufficient balance
      ├─ 3. Save Transaction with status: PENDING_FRAUD_CHECK
      │
      └─ 4. [After DB commit] Publish → Kafka topic: transaction.initiated
                                                │
                                    Fraud Service consumes (Python)
                                                │
                                    Evaluate rules → risk score
                                                │
                                    Publish → Kafka topic: fraud.assessment
                                                │
                                    Transaction Service consumes (Java)
                                                │
                              APPROVED → pessimistic lock → debit/credit → COMPLETED
                              REJECTED → FRAUD_REJECTED (no money moves)
```

The transfer endpoint returns **202 Accepted** immediately. Poll `GET /api/transfers/{id}` to see the final status (typically resolves in under 1 second).

---

## Fraud Detection Rules

Fraud rules live in MongoDB (`fraud_db.fraud_rules`) and can be updated at runtime via `POST /fraud/rules/`.

| Rule | Condition | Risk Score Added |
|---|---|---|
| `AMOUNT_THRESHOLD` | Transfer amount > **$10,000** | +50 |
| `VELOCITY_CHECK` | More than **5 transactions** from same account in 60 minutes | +40 |
| `BLOCKED_ACCOUNT` | From/to account is on the blocked list | +100 |

**Decision:** `risk_score >= 70` → **REJECTED**, otherwise → **APPROVED**

| Score | Result | Example trigger |
|---|---|---|
| 0 | APPROVED | Normal small transfer |
| 50 | APPROVED (flagged) | Single large transfer |
| 90 | **REJECTED** | Large transfer + high velocity |
| 100 | **REJECTED** | Blocked account involved |

---

## Running Tests

```bash
# --- Java (Transaction Service) ---
cd transaction-service

# Unit tests only (fast, no Docker needed)
mvn test

# Integration tests (spins up Testcontainers — requires Docker)
mvn verify -P integration-tests


# --- Python (Fraud Service) ---
cd fraud-service

# Install deps (one-time)
pip install -r requirements.txt

# Run tests
pytest -v

# Lint
ruff check .


# --- Frontend ---
cd frontend

# Install deps (one-time)
npm install

# TypeScript type check
npx tsc --noEmit

# Production build
npm run build

# Dev server with hot reload (proxies /api → localhost:8080)
npm run dev
# then open http://localhost:3000
```

---

## Stopping & Resetting

```bash
# Stop all containers, keep database data
docker compose down

# Stop and WIPE all data (Postgres + MongoDB volumes)
# Use this if Flyway migrations are out of sync or you want a clean slate
docker compose down -v

# Rebuild a single service after code changes
docker compose build transaction-service
docker compose up -d transaction-service

# View logs for a crashing service
docker compose logs transaction-service --tail=50
```

---

## Architecture

```
┌─────────────┐   REST    ┌──────────────────────┐
│   Frontend  │ ────────▶ │  Transaction Service  │
│  React + TS │           │  Java 21 / Spring     │
│  :3000      │           │  Boot 3 / JWT         │
└─────────────┘           │  PostgreSQL + Redis   │
                          │  :8080                │
                          └──────────┬────────────┘
                                     │ Kafka: transaction.initiated
                                     ▼
                          ┌──────────────────────┐
                          │   Fraud Service      │
                          │   Python 3.12 /      │
                          │   FastAPI + aiokafka │
                          │   MongoDB (fraud_db) │
                          │   :8090              │
                          └──────────┬───────────┘
                                     │ Kafka: fraud.assessment
                                     ▼
                          ┌──────────────────────┐
                          │  Transaction Service │
                          │  applies decision,   │
                          │  updates balances    │
                          └──────────────────────┘
```

**Key design decisions:**
- Services communicate **only via Kafka** — no direct HTTP calls between microservices
- Kafka publish happens **outside `@Transactional`** — prevents publishing events for rolled-back writes
- Fraud service is **idempotent** — re-processing the same `transaction_id` returns the cached result without re-evaluating
- Balance reads use **Redis cache** (TTL 60s), evicted on every fund movement
- Fund transfers use **pessimistic DB locking** (`SELECT FOR UPDATE`) to prevent race conditions

---

## JD Skills Demonstrated

| Skill | Where |
|---|---|
| Java 21 + Spring Boot 3 | `transaction-service/` |
| REST API design + OpenAPI 3 | `TransactionController`, Swagger UI |
| JWT authentication + RBAC | `JwtFilter`, `SecurityConfig` |
| Spring Data JPA + PostgreSQL | `AccountRepository`, `TransactionRepository` |
| Flyway database migrations | `src/main/resources/db/migration/` |
| MongoDB (audit log) | `TransactionEvent`, `AuditService` |
| Redis caching | `RedisConfig`, `@Cacheable` on balance |
| Apache Kafka event streaming | `TransactionEventProducer`, `FraudAssessmentConsumer` |
| Event-driven microservices | Kafka-only inter-service comms, no HTTP coupling |
| Python 3.12 + FastAPI | `fraud-service/` |
| Async Python (asyncio / aiokafka) | `kafka_service.py` |
| Pydantic v2 validation | `FraudAssessment`, `FraudRule` models |
| Idempotency | Idempotency keys (TX service), duplicate-check (Fraud service) |
| Pessimistic locking | `findByIdForUpdate` in `AccountRepository` |
| State machine | `TransactionStatus.canTransitionTo()` |
| Testcontainers integration tests | Maven `integration-tests` profile |
| React 18 + TypeScript | `frontend/src/` |
| Tailwind CSS | All components and pages |
| Docker multi-stage builds | `transaction-service/Dockerfile`, `frontend/Dockerfile` |
| Docker Compose orchestration | `docker-compose.yml` (11 services with healthchecks) |
| GitHub Actions CI/CD | `.github/workflows/ci.yml` |
