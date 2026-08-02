# Service Tour — Payment Transaction Service

A guided walkthrough of all 11 running services: what each one does, how to log in, what you can do inside it, and how they all connect.

---

## Quick Reference — Credentials

| Service | URL | Username | Password | Notes |
|---|---|---|---|---|
| Frontend (Alice) | http://localhost:3000 | `alice@example.com` | `customer123` | CUSTOMER role |
| Frontend (Bob) | http://localhost:3000 | `bob@example.com` | `customer123` | CUSTOMER role |
| Frontend (Admin) | http://localhost:3000 | `admin@bank.com` | `admin123` | BANK_ADMIN role |
| Swagger UI | http://localhost:8080/swagger-ui/index.html | _(use JWT from login)_ | — | Paste token into Authorize |
| FastAPI Docs | http://localhost:8090/docs | _(no auth required)_ | — | Open access |
| Kafka UI | http://localhost:8082 | _(no auth required)_ | — | Open access |
| Adminer | http://localhost:8888 | `payments` | `payments` | System=PostgreSQL, Server=`postgres`, DB=`payments_db` |
| Mongo Express | http://localhost:8081 | _(no auth required)_ | — | Open access |

---

## Service 1 — Frontend (React SPA)

**URL:** http://localhost:3000

### Login credentials

| Field | Value |
|---|---|
| Email | `alice@example.com` |
| Password | `customer123` |

You can also log in as Bob (`bob@example.com` / `customer123`) or the bank admin (`admin@bank.com` / `admin123`).

### What you see after login

Alice's dashboard showing her account **ACC-ALICE-001** with a live USD balance fetched from the Transaction Service.

### What you can do

| Action | How |
|---|---|
| View live balance | Shown automatically on the dashboard card (reads from Redis cache via Transaction Service) |
| Send a transfer | Click **New Transfer** → enter the destination account UUID + amount + optional description → Submit |
| View transaction history | Click **History** in the navbar → lists all past transactions with status badges (COMPLETED / FRAUD_REJECTED / PENDING) |
| Logout | Click **Logout** in the top-right navbar — clears the JWT from localStorage |

### Role in the system

The only service that talks directly to end users. It holds a JWT in localStorage and attaches it as `Authorization: Bearer <token>` on every API request to the Transaction Service. It never talks to the Fraud Service or any infrastructure service.

---

## Service 2 — Transaction Service (Spring Boot REST API)

**URL:** http://localhost:8080  
**No browser UI** — access via the Frontend or Swagger UI below.

### Role in the system

The core backend. It:
1. Issues JWTs on `POST /auth/login`
2. Reads/writes account balances in PostgreSQL
3. Caches balance reads in Redis (60-second TTL)
4. Validates and persists transfer requests
5. Publishes `transaction.initiated` events to Kafka to trigger fraud checks
6. Consumes `fraud.assessment` events from Kafka and marks transactions COMPLETED or FRAUD_REJECTED
7. Writes an audit entry to MongoDB (`payments_db.transaction_events`) on every status change

---

## Service 3 — Swagger UI (Transaction Service API Docs)

**URL:** http://localhost:8080/swagger-ui/index.html  
**No login required to browse.** Authentication needed to call protected endpoints.

### How to authenticate

1. Open `POST /auth/login` → click **Try it out** → enter:
   ```json
   { "email": "alice@example.com", "password": "customer123" }
   ```
2. Copy the `token` value from the response body.
3. Click the **Authorize** button (top-right padlock icon).
4. Paste the token (without quotes) → click **Authorize** → **Close**.

### What you can do

| Endpoint | Description |
|---|---|
| `POST /auth/login` | Get a JWT token |
| `POST /auth/register` | Register a new user |
| `GET /accounts/{id}` | Fetch account details and current balance |
| `POST /transactions/transfer` | Initiate a money transfer between two accounts |
| `GET /transactions` | List all transactions for an account (`?accountId=<uuid>`) |
| `GET /transactions/{id}` | Get a single transaction by ID |

### Role in the system

Developer tooling — auto-generated from `@Operation` annotations in the Spring Boot source. Lets QA engineers and frontend developers discover and test the API without Postman.

---

## Service 4 — Fraud Detection Service (FastAPI Docs)

**URL:** http://localhost:8090/docs  
**No login required.**

### What you can do

| Endpoint | Description |
|---|---|
| `GET /fraud/rules/` | List all active fraud rules |
| `POST /fraud/rules/` | Add or update a fraud rule (e.g., lower the AMOUNT_THRESHOLD to $500) |
| `GET /fraud/assessments/{transaction_id}` | Look up the fraud verdict for a specific transaction UUID |
| `GET /fraud/alerts/` | List all transactions flagged as FRAUD_REJECTED |
| `GET /health` | Returns `{"status":"ok"}` — confirms the service is alive |

### Built-in fraud rules (seeded on startup)

| Rule | Trigger | Action |
|---|---|---|
| `AMOUNT_THRESHOLD` | Transfer amount > $10,000 | Risk score +60 |
| `VELOCITY_CHECK` | More than 3 transfers within 1 hour | Risk score +30 |
| `BLOCKED_ACCOUNT` | Source account is on a block list | Risk score +100 |

A total risk score ≥ 70 → transaction is marked **FRAUD_REJECTED**.

### Role in the system

An autonomous Python microservice. It **never calls the Transaction Service directly** — it only:
1. Consumes `transaction.initiated` events from Kafka
2. Evaluates the three rules against the transaction amount and recent history stored in MongoDB
3. Writes the verdict to `fraud_db.fraud_assessments`
4. Publishes a `fraud.assessment` event back to Kafka so the Transaction Service can act on it

---

## Service 5 — Kafka UI

**URL:** http://localhost:8082  
**No login required.**

### What you see

A dark-themed dashboard showing the `local` Kafka cluster. Navigate via the left sidebar.

### Business topics

| Topic | Messages | Producer | Consumer |
|---|---|---|---|
| `transaction.initiated` | 6 | Transaction Service | Fraud Service |
| `fraud.assessment` | 6 | Fraud Service | Transaction Service |
| `transaction.completed` | 6 | Transaction Service | _(future downstream services)_ |

> `__consumer_offsets` is an internal Kafka topic — ignore it.

### What you can do

| Section | What to see |
|---|---|
| **Topics** → click a topic → **Messages** tab | Browse every event payload in JSON, with offset, partition, and timestamp — watch fraud decisions flow in real-time |
| **Consumers** in the sidebar | See `fraud-detection-group` consumer group and its lag (0 = up to date, non-zero = processing backlog) |
| **Brokers** | Single broker `kafka:9092`, Kafka 7.6.1 |
| **Add a Topic** button | Create a new Kafka topic manually |

### Role in the system

The async message backbone. Services **never call each other via HTTP** — all inter-service communication happens through Kafka topics. This is what makes the architecture event-driven and loosely coupled.

---

## Service 6 — Adminer (PostgreSQL Web Client)

**URL:** http://localhost:8888

### Login credentials

| Field | Value |
|---|---|
| System | `PostgreSQL` |
| Server | `postgres` |
| Username | `payments` |
| Password | `payments` |
| Database | `payments_db` |

### Tables in `payments_db.public`

| Table | Contents |
|---|---|
| `users` | 3 rows — admin, alice, bob with BCrypt-hashed passwords and roles |
| `accounts` | 2 rows — ACC-ALICE-001 and ACC-BOB-001 with current USD balances |
| `transactions` | All transfer records with `status` column (PENDING / COMPLETED / FRAUD_REJECTED) |
| `flyway_schema_history` | Flyway migration log — V1 (users), V2 (accounts), V3 (transactions) applied in order |

### What you can do

| Action | How |
|---|---|
| Browse tables | Click **select** next to any table name |
| Run a custom query | Click **SQL command** in the left panel, write any SQL, click **Execute** |
| Check current balances | `SELECT account_number, balance, currency FROM accounts;` |
| See rejected transfers | `SELECT * FROM transactions WHERE status = 'FRAUD_REJECTED';` |
| Inspect a user's data | `SELECT id, email, role FROM users;` |

### Role in the system

The Transaction Service's source-of-truth relational store. All account balance mutations are performed inside DB transactions to guarantee ACID consistency. Flyway manages schema versioning — migrations run automatically on service startup.

---

## Service 7 — Mongo Express (MongoDB Web Client)

**URL:** http://localhost:8081  
**No login required.**

### Databases

| Database | Owner | Collection | Contents |
|---|---|---|---|
| `fraud_db` | Fraud Service | `fraud_assessments` | One document per fraud evaluation — transaction ID, risk score, decision, matched rules, timestamp |
| `fraud_db` | Fraud Service | `fraud_rules` | The three active fraud rules and their thresholds |
| `payments_db` | Transaction Service | `transaction_events` | Audit log — one document per transaction status change written by Spring Boot |

### What you can do

| Action | How |
|---|---|
| Browse fraud verdicts | Click **fraud_db** → **fraud_assessments** → **View** |
| See active rules | Click **fraud_db** → **fraud_rules** → **View** |
| Check audit trail | Click **payments_db** → **transaction_events** → **View** |
| Edit a document | Click the pencil icon next to any document |
| Query by field | Use the **Filter** field (MongoDB query syntax, e.g. `{"decision":"FRAUD_REJECTED"}`) |

### Role in the system

Document store for non-relational data. Two separate services write to two separate databases — the microservice **database-per-service** pattern. Neither service can read the other's MongoDB database.

---

## Infrastructure Services (No UI)

These three services run silently in the background and are accessed only by the application services above.

| Service | Port | Role |
|---|---|---|
| **PostgreSQL 16** | 5432 | Relational store for users, accounts, transactions (owned by Transaction Service) |
| **MongoDB 7** | 27017 | Document store for fraud assessments and audit events |
| **Redis 7** | 6379 | Balance cache with 60-second TTL (owned by Transaction Service) |
| **Apache Kafka** | 9092 | Async event bus between Transaction Service and Fraud Service |
| **Zookeeper** | 2181 | Kafka cluster coordinator (managed by Kafka, not touched directly) |

---

## How All Services Connect

```
Browser
  └──► (3000) Frontend (React)
              │
              │  JWT REST (Authorization: Bearer <token>)
              ▼
       (8080) Transaction Service (Spring Boot)
              │
              ├──► PostgreSQL :5432  ◄── Adminer :8888
              │    users, accounts,
              │    transactions
              │
              ├──► Redis :6379
              │    balance cache, TTL 60s
              │
              ├──► MongoDB :27017    ◄── Mongo Express :8081
              │    payments_db.transaction_events
              │    (audit log)
              │
              ├──PUBLISH──► Kafka :9092  ◄── Kafka UI :8082
              │             topic: transaction.initiated
              │                    │
              │             (8090) Fraud Service (Python / FastAPI)
              │                    │
              │                    ├──► MongoDB :27017
              │                    │    fraud_db.fraud_assessments
              │                    │    fraud_db.fraud_rules
              │                    │
              │                    └──PUBLISH──► Kafka :9092
              │                                 topic: fraud.assessment
              │
              └──CONSUME◄── Kafka :9092
                            topic: fraud.assessment
                            (marks transaction COMPLETED or FRAUD_REJECTED)
```

**Swagger UI** (`:8080/swagger-ui`) and **FastAPI Docs** (`:8090/docs`) are documentation layers on top of their services — they have no data path.

---

## Transfer Flow — Step by Step

1. Alice clicks **New Transfer** in the Frontend → `POST /transactions/transfer` hits the Transaction Service
2. Transaction Service validates the request, deducts Alice's balance, creates a `PENDING` transaction in PostgreSQL
3. Transaction Service publishes a `transaction.initiated` Kafka event
4. Fraud Service consumes the event, evaluates amount + velocity + blocklist rules, stores result in MongoDB
5. Fraud Service publishes a `fraud.assessment` Kafka event with the verdict (APPROVED / FRAUD_REJECTED)
6. Transaction Service consumes the assessment → updates PostgreSQL status to COMPLETED or FRAUD_REJECTED
7. Alice refreshes the **History** page → sees the final status
