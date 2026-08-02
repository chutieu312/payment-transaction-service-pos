# Payment Transaction Service — Practice Project Plan (v2 — Microservices + Kafka)

## JD Summary

**Company:** FPT Americas (FPT Software overseas branch)  
**Role:** Senior Software Engineer — Contractor/Full-time — Onsite, Costa Mesa, CA  
**Domain:** Enterprise IT services — key vertical: **Banking, Financial Services** (BFSI)  
**Core Stack:** Java 8–21, Spring Boot, J2EE, PostgreSQL, MongoDB, Redis, Jenkins, Git, Docker  
**Candidate background:** Prior experience at JP Morgan — strong fit for BFSI domain

---

# 1. JD Skill Extraction

## Required Technical Skills
- Java 1.8 through Java 21
- Spring Boot framework
- J2EE standards and best practices
- Relational databases: Oracle, PostgreSQL
- NoSQL databases: MongoDB, Redis
- Version control: Git, GitHub, GitLab
- CI/CD: Jenkins (build automation, pipeline configuration)
- SDLC, Agile/Scrum methodology
- Collaboration tools: Jira, Confluence, Atlassian Suite
- CS fundamentals: Arrays, Linked Lists, Trees, Graphs, Hash Tables

## Preferred Technical Skills
- Microservices architecture
- RESTful API design and implementation
- Docker and containerization
- Cloud platforms: AWS, Azure, GCP
- Software security best practices
- Unit testing with JUnit and Mockito

## Soft Skills and Collaboration Expectations
- Independent contributor, minimal supervision required
- Quick onboarding, self-learner
- Accountability: owns features from design through delivery
- Strong analytical and troubleshooting skills
- Clear stakeholder communication
- Quality-focused: performance, security, maintainability
- Adaptable in dynamic environments

---

# 2. Skill Categories

| Category | Skills |
|---|---|
| **Backend** | Java 21 (Spring Boot 3.x), Python 3.12 (FastAPI), J2EE patterns, Spring Security, REST API, microservices |
| **Messaging** | Apache Kafka (async event streaming between services) |
| **Frontend** | React 18 + TypeScript, Tailwind CSS, Vite |
| **Database** | PostgreSQL (Transaction Service primary), MongoDB (Fraud Service primary), Redis (idempotency + cache) |
| **Cloud** | AWS (EC2 + RDS + ElastiCache + MSK) as deployment target |
| **DevOps / CI/CD** | Docker, docker-compose, GitHub Actions (multi-service parallel pipeline) |
| **Testing** | JUnit 5, Mockito, MockMvc, Testcontainers (Java); pytest, pytest-asyncio (Python) |
| **Security** | JWT Bearer tokens, Spring Security, RBAC, idempotency keys |
| **AI Tools / Automation** | GitHub Copilot |
| **Other** | Swagger UI, FastAPI built-in docs, Kafka UI, Adminer, Mongo Express, Storybook |

---

# 3. Recommended Mini Project

## Payment Transaction Service + Fraud Detection Service

A two-microservice banking system:

1. **Transaction Service** (Java 21 / Spring Boot) — the core payment API. Handles accounts, initiates fund transfers, enforces idempotency, publishes and consumes Kafka events.
2. **Fraud Detection Service** (Python 3.12 / FastAPI) — a standalone analytical service. Consumes transaction events from Kafka, evaluates configurable fraud rules, stores assessments in MongoDB, and publishes its decision back to Kafka.

The two services never call each other directly. All communication is asynchronous through Kafka.

### Why this project?
- Two services with genuinely different stacks (Java + Python) is the textbook microservices demonstration
- Kafka is the industry standard for async event streaming in fintech — JP Morgan processes billions of events per day on Kafka infrastructure
- Fraud detection as a decoupled service is real-world architecture: fraud checks must not block payment API latency
- Polyglot persistence: Java service owns PostgreSQL, Python service owns MongoDB — each service has its own data store
- Python is the dominant language for risk and fraud analysis in banking, matching the real-world division of responsibilities
- Every section of this project is explainable from direct JP Morgan experience

---

# 4. Why This Project Matches the JD

| JD Responsibility | Project Coverage |
|---|---|
| Design scalable Java 8-21 applications | Spring Boot 3.x (Java 21) for Transaction Service |
| Build microservices with Spring Boot | Two independent, separately deployable services communicating only via Kafka |
| J2EE enterprise patterns | Repository, Service layer, DTO/Mapper, @Transactional for atomic balance updates |
| Relational + NoSQL databases | PostgreSQL (Transaction Service), MongoDB (Fraud Service primary), Redis (cache + idempotency) |
| CI/CD | GitHub Actions: multi-job pipeline for both services in parallel |
| JUnit + Mockito + Python tests | Java: JUnit 5 + Mockito + Testcontainers; Python: pytest + pytest-asyncio |
| Security best practices | JWT auth, RBAC, idempotency keys, input validation |
| CS Fundamentals Graphs | Transaction status state machine as directed graph adjacency map |
| Microservices architecture | Two independently deployable services, event-driven coupling via Kafka |
| Quick productivity / self-learner | Python service demonstrates picking up a second language stack in the same project |

---

# 5. Architecture Overview



---

## Async Transaction Flow



---

## Transaction Status State Machine



---

# 6. Tech Stack Mapping

| JD Skill | Project Feature | How to Explain in an Interview |
|---|---|---|
| Java 21 | Transaction Service: records for DTOs, sealed TransactionStatus enum | "I used Java 21 records for immutable DTOs and a sealed enum for transaction status so the compiler enforces exhaustive case handling." |
| Spring Boot | Full REST API: auto-config, embedded Tomcat, Actuator, Spring Data | "Spring Boot 3 handles server, security, data access, and health checks. Controller -> Service -> Repository layers." |
| J2EE Patterns | Repository, Service, DTO/Mapper, @Transactional on debit+credit | "@Transactional wraps debit and credit in one atomic unit. If the credit fails, the debit rolls back automatically." |
| PostgreSQL | accounts, transactions, users; Flyway migrations | "Flyway gives reproducible schema migrations. Every environment runs the same history — critical for auditable financial data." |
| MongoDB | Fraud Service: fraud_rules and fraud_assessments (primary); Transaction Service: transaction_events (audit) | "Fraud rules have flexible structure. MongoDB fits naturally. The transaction event log is append-only and immutable." |
| Redis | Idempotency keys TTL 24h; balance cache TTL 1min | "Idempotency keys in Redis prevent duplicate transfers on retry. Balance cache reduces read load on the hot GET /balance path." |
| Apache Kafka | transaction.initiated -> fraud check; fraud.assessment -> transaction completion | "The two services never call each other directly. Transaction Service publishes an event, Fraud Service consumes asynchronously and publishes its decision back. Loose coupling, independent deployability." |
| Microservices | Two independently deployable services, separate DBs, no shared code | "Each service owns its data store and deployment lifecycle. The only contract between them is the Kafka event schema." |
| Python + FastAPI | Fraud Detection Service with built-in OpenAPI at /docs | "Python is the natural choice for analytical/rules-based services. FastAPI gives async request handling and zero-config Swagger at /docs." |
| Git / GitHub | Feature branches, PR workflow, conventional commits | "Feature branches off main, PR before merge, conventional commits so changelog is automatable." |
| CI/CD Jenkins-equiv | GitHub Actions: parallel Java + Python test jobs -> Docker build -> push | "Java and Python tests run in parallel. The build stage only runs after both pass — equivalent to Jenkins parallel stages with a join gate." |
| JUnit + Mockito | Java: service unit tests, MockMvc controller tests, Testcontainers | "Mockito mocks repositories for fast isolated tests. Testcontainers spins up real PostgreSQL + Kafka for integration tests." |
| pytest | Python: fraud rules unit tests, Kafka event processing tests | "Each fraud rule is tested independently. pytest-asyncio covers the async Kafka consumer code." |
| Docker | Multi-stage Dockerfiles for both services; full stack in docker-compose | "Everything starts with docker compose up. Java uses multi-stage JDK/JRE build. Python uses slim python:3.12-slim base." |
| Security | JWT auth, RBAC, idempotency, input validation | "JWT filter + Bean Validation guards Transaction Service. Fraud Service is internal-only — not exposed outside Docker network." |
| Cloud AWS | EC2/ECS + RDS + ElastiCache + MSK + DocumentDB | "RDS for PostgreSQL, ElastiCache for Redis, MSK for Kafka. Fraud Service runs as a separate ECS task — independently scalable by consumer lag." |
| CS Fundamentals Graphs | Transaction status state machine as directed graph adjacency map | "Valid status transitions are a directed graph modeled as Map<Status, Set<Status>>. Invalid transition returns 409." |

---

# 7. Step-by-Step Build Plan

## Phase 1: Transaction Service — Backend Foundation
**Goal:** Spring Boot REST API with JWT auth and account/transfer CRUD

- Spring Initializr: Spring Web, Spring Security, Spring Data JPA, Spring Data MongoDB, Spring Data Redis, Validation, Lombok, springdoc-openapi, Flyway
- Domain models: Account, Transaction, User
- TransactionStatus sealed enum: PENDING_FRAUD_CHECK, PROCESSING, COMPLETED, FAILED, FRAUD_REJECTED, REVERSED
- JWT auth: AuthController, JWT filter, UserDetailsService
- AccountController, TransactionController (POST /transfers returns 202 Accepted)
- Role-based access: CUSTOMER / BANK_ADMIN
- @RestControllerAdvice with RFC-7807 error responses

**JD Skills:** Java 21, Spring Boot, J2EE patterns, Spring Security, REST API

## Phase 2: Transaction Service — Database Layer
**Goal:** PostgreSQL with Flyway, Redis idempotency + cache, MongoDB audit log

- PostgreSQL: Flyway migrations for accounts, transactions, users
- Redis: idempotency keys TTL 24h, @Cacheable("balances") with @CacheEvict
- MongoDB: append-only TransactionEvent documents per status change

**JD Skills:** PostgreSQL, MongoDB, Redis, J2EE data patterns

## Phase 3: Kafka Integration — Transaction Service Producer + Consumer
**Goal:** Transaction Service publishes and consumes Kafka events

- Add spring-kafka dependency
- KafkaProducerConfig: serialize TransactionInitiatedEvent as JSON
- On POST /transfers:
  1. Persist transaction with status PENDING_FRAUD_CHECK
  2. Publish TransactionInitiatedEvent to topic transaction.initiated
  3. Return 202 Accepted
- @KafkaListener on topic fraud.assessment:
  - If APPROVED: update to PROCESSING -> execute debit/credit -> COMPLETED
  - If REJECTED: update to FRAUD_REJECTED
  - Publish TransactionCompletedEvent to transaction.completed

**Kafka Event Schemas:**

transaction.initiated:


fraud.assessment (consumed by Transaction Service):


**JD Skills:** Microservices, async communication, event-driven architecture

## Phase 4: Fraud Detection Service — Python + FastAPI Foundation
**Goal:** Standalone Python service with fraud rule evaluation

- Project init: python -m venv .venv, pip install fastapi uvicorn motor aiokafka pydantic
- MongoDB collections:
  - fraud_rules: seeded with default rules on startup
  - fraud_assessments: one document per evaluated transaction
- Pydantic models: FraudRule, FraudAssessment, TransactionInitiatedEvent
- FastAPI REST endpoints:
  - GET /fraud/rules
  - POST /fraud/rules
  - GET /fraud/assessments/{transactionId}
  - GET /fraud/alerts (REJECTED assessments)
- Built-in Swagger UI at localhost:8090/docs — zero config

**Default Fraud Rules:**

| Rule | Condition | Risk Score Added |
|---|---|---|
| AMOUNT_THRESHOLD | amount > 0,000 | +50 |
| VELOCITY_CHECK | >5 transactions from same account in last 60 min | +40 |
| BLOCKED_ACCOUNT | source or destination account on blocklist | +100 (auto-reject) |
| Decision | risk_score >= 70 -> REJECTED, else APPROVED | — |

**JD Skills:** Microservices, Python, REST API, MongoDB, polyglot persistence

## Phase 5: Kafka Integration — Fraud Detection Service Consumer + Producer
**Goal:** Fraud Service subscribes to Kafka, evaluates, publishes decision

- aiokafka async consumer: subscribe to transaction.initiated
- On each event:
  1. Parse TransactionInitiatedEvent
  2. Run FraudRulesEngine.evaluate(event) against rules in MongoDB
  3. Compute total risk score and decision
  4. Store FraudAssessment in MongoDB
  5. Publish FraudAssessmentEvent to fraud.assessment topic
- Start consumer in background via FastAPI @asynccontextmanager lifespan

**Idempotency note for Kafka at-least-once delivery:**
Before evaluating, check if a FraudAssessment already exists for this transactionId in MongoDB. If yes, re-publish the existing result without re-evaluating. This makes the consumer idempotent.

**JD Skills:** Kafka, async processing, event-driven microservices, Python

## Phase 6: Frontend UI
**Goal:** React dashboard showing the async fraud-check flow

- LoginPage, AccountDashboard, TransferForm
- TransferForm submits -> shows PENDING_FRAUD_CHECK badge immediately (202 Accepted)
- Polling on GET /transfers/{id} -> badge updates as status changes
- TransactionHistory with status timeline
- AdminPanel: view fraud alerts from Fraud Detection Service GET /fraud/alerts

**JD Skills:** Frontend, REST API consumption, async UI patterns

## Phase 7: Testing
**Goal:** Reliable tests for both services

**Java (Transaction Service):**
- TransactionServiceTest (JUnit 5 + Mockito): mock Kafka producer, test idempotency, test state machine
- TransactionControllerTest (MockMvc): 202 on valid transfer, 422 on negative amount, 409 on bad transition
- KafkaIntegrationTest (EmbeddedKafka + Testcontainers): publish event -> verify consumer receives it
- TransactionRepositoryIT (Testcontainers): Flyway migrations, balance locking

**Python (Fraud Detection Service):**
- test_fraud_rules.py (pytest): each rule independently with mock data
- test_fraud_engine.py (pytest): combined scoring, boundary (score 69 vs 70)
- test_kafka_consumer.py (pytest-asyncio): mock Kafka event -> assess -> assert MongoDB stored + correct event published
- test_api.py (pytest + httpx TestClient): all FastAPI endpoints

**JD Skills:** JUnit, Mockito, pytest, Testcontainers, quality focus

## Phase 8: Docker
**Goal:** Full two-service stack with Kafka in one docker compose up

Services in docker-compose.yml:
- transaction-service (Spring Boot, port 8080)
- fraud-service (FastAPI/uvicorn, port 8090)
- zookeeper (port 2181)
- kafka (confluentinc/cp-kafka, port 9092)
- kafka-ui (provectuslabs/kafka-ui, port 8082)
- postgres (port 5432)
- redis (port 6379)
- mongodb (port 27017)
- adminer (port 8888)
- mongo-express (port 8081)

**JD Skills:** Docker, containerization, microservices deployment

## Phase 9: CI/CD
**Goal:** Parallel multi-service pipeline



**JD Skills:** CI/CD, multi-service pipeline, DevOps

## Phase 10: Cloud Deployment Plan

| Local (docker-compose) | AWS Equivalent |
|---|---|
| kafka + zookeeper | Amazon MSK (Managed Streaming for Kafka) |
| postgres | Amazon RDS PostgreSQL |
| redis | Amazon ElastiCache |
| mongodb | MongoDB Atlas M0 or AWS DocumentDB |
| transaction-service | ECS Fargate Task A |
| fraud-service | ECS Fargate Task B (independently scalable by Kafka consumer lag) |
| frontend | S3 + CloudFront |

**JD Skills:** Cloud platforms AWS, production readiness

## Phase 11: Developer Exploration Tools
**Goal:** Any new hire can explore the full running system instantly

| Tool | URL | Purpose |
|---|---|---|
| Swagger UI (Transaction) | localhost:8080/swagger-ui.html | Try Java REST endpoints with JWT |
| FastAPI Docs (Fraud) | localhost:8090/docs | Try Fraud Service endpoints, zero config |
| Kafka UI | localhost:8082 | Browse topics, inspect messages, view consumer lag |
| Adminer | localhost:8888 | Browse PostgreSQL accounts + transactions |
| Mongo Express | localhost:8081 | Browse fraud_rules, fraud_assessments, transaction_events |
| Storybook | localhost:6006 | Explore React components (npm run storybook) |

**JD Skills:** API documentation, developer experience, onboarding speed

## Phase 12: Interview Demo Script
**Goal:** 15-minute end-to-end walkthrough

1. Open Swagger UI (Transaction Service) -> POST /auth/login -> Authorize
2. Initiate a small transfer (00) -> 202 Accepted, status: PENDING_FRAUD_CHECK
3. Open Kafka UI -> transaction.initiated topic -> show the event message live
4. Watch Kafka UI -> fraud.assessment topic -> show APPROVED decision (score < 70)
5. Poll GET /transfers/{id} -> status is now COMPLETED
6. Open FastAPI Docs (Fraud Service :8090/docs) -> GET /fraud/assessments/{id} -> show risk score stored in MongoDB
7. Initiate a large transfer (5,000) -> 202 Accepted
8. Show Kafka event -> fraud.assessment -> REJECTED (AMOUNT_THRESHOLD_EXCEEDED)
9. Show GET /transfers/{id} -> status: FRAUD_REJECTED
10. Update fraud rule via POST /fraud/rules -> change threshold to ,000
11. Show @Transactional in TransactionService.java -> explain atomicity
12. Show KafkaConsumerService.py -> explain event-driven decoupling
13. Show CI pipeline -> GitHub Actions -> parallel Java + Python test jobs

---

# 8. Developer Exploration Tools (Detail)

### API Explorer 1: Swagger UI (Transaction Service — Spring Boot)
- Dependency: springdoc-openapi-starter-webmvc-ui
- URL: localhost:8080/swagger-ui.html
- JWT flow: POST /auth/login -> copy token -> Authorize -> Bearer <token>

### API Explorer 2: FastAPI Built-in Docs (Fraud Detection Service)
- Zero configuration required — FastAPI generates OpenAPI automatically
- Swagger UI at localhost:8090/docs
- ReDoc at localhost:8090/redoc
- No auth needed — fraud service is internal Docker network only

### Kafka Explorer: Kafka UI
- Image: provectuslabs/kafka-ui:latest
- Port: 8082
- Capabilities: browse topics, inspect message payload and headers, replay messages, view consumer group lag



### Database Admin: Adminer (PostgreSQL)
- Image: adminer:4, Port: 8888
- Login: PostgreSQL, server: postgres, user: payments_user, db: payments_db

### Database Admin: Mongo Express (MongoDB)
- Image: mongo-express, Port: 8081
- Browses both: payments_db (transaction_events) and fraud_db (rules + assessments)

### Component Explorer: Storybook
- npm run storybook -> localhost:6006
- Stories: BalanceCard, TransactionRow, StatusBadge, TransferForm

---

# 9. Project Structure



---

# 10. Local Setup Commands

```bash
# 1. Clone and enter the project
git clone https://github.com/<your-username>/payment-transaction-service
cd payment-transaction-service

# 2. Copy environment variables
cp .env.example .env

# 3. Start entire stack
#    Transaction Service + Fraud Service + Kafka + Zookeeper +
#    PostgreSQL + Redis + MongoDB + Kafka UI + Adminer + Mongo Express
docker compose up --build

# 4. All services available at:
#   Transaction API:         http://localhost:8080
#   Swagger UI (Java):       http://localhost:8080/swagger-ui.html
#   Fraud API:               http://localhost:8090
#   FastAPI Docs (Python):   http://localhost:8090/docs
#   Kafka UI:                http://localhost:8082
#   Adminer (PostgreSQL):    http://localhost:8888
#   Mongo Express (MongoDB): http://localhost:8081
#   Frontend:                http://localhost:3000

# 5. Run Java unit tests
cd transaction-service && ./mvnw test

# 6. Run Java integration tests (Testcontainers -- Docker must be running)
./mvnw verify -P integration-tests

# 7. Run Python tests
cd fraud-service
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scriptsctivate
pip install -r requirements.txt
pytest

# 8. Run Python linting
ruff check .

# 9. Run frontend tests
cd frontend && npm ci && npm test

# 10. Start Storybook locally
cd frontend && npm run storybook   # -> http://localhost:6006

# 11. Watch Kafka events live
#     Open http://localhost:8082 -> Topics -> transaction.initiated
```

---

# 11. Testing Plan

| Test Type | Service | Tool | What Is Tested |
|---|---|---|---|
| Unit TransactionService | Java | JUnit 5 + Mockito | Transfer logic, idempotency, state machine, Kafka producer mocked |
| Unit FraudAssessmentConsumer | Java | JUnit 5 + Mockito | APPROVED event -> COMPLETED path; REJECTED -> FRAUD_REJECTED |
| Controller tests | Java | MockMvc | 202 on valid transfer, 422 on bad input, 403 on wrong role |
| Kafka integration | Java | EmbeddedKafka + Testcontainers | Transaction Service publishes event -> verify message in topic |
| DB integration | Java | Testcontainers PostgreSQL | Flyway migrations, balance locking under concurrent writes |
| Unit fraud_engine | Python | pytest | Each rule independently: amount threshold, velocity window, blocklist |
| Unit scoring | Python | pytest | Score 69 -> APPROVED; score 70 -> REJECTED; boundary conditions |
| Kafka consumer | Python | pytest-asyncio + mock aiokafka | Fake Kafka message -> assess -> assert MongoDB stored + correct event published |
| API endpoint tests | Python | pytest + httpx TestClient | GET /fraud/rules, POST /fraud/rules, GET /fraud/assessments/{id} |
| Frontend | React | Jest + RTL | TransferForm validation, StatusBadge colors, polling behavior |

---

# 12. CI/CD Plan

```yaml
# .github/workflows/ci.yml
on: [push, pull_request]

jobs:
  test-java:                         # runs in parallel with test-python
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21' }
      - run: cd transaction-service && ./mvnw test
      - run: cd frontend && npm ci && npm test

  test-python:                       # runs in parallel with test-java
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: '3.12' }
      - run: cd fraud-service && pip install -r requirements.txt && pytest
      - run: cd fraud-service && ruff check .

  integration-tests:
    needs: [test-java, test-python]  # only runs if both pass
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21' }
      - run: cd transaction-service && ./mvnw verify -P integration-tests
        # Testcontainers spins up PostgreSQL + Redis + Kafka containers

  build-and-push:
    needs: integration-tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with: { registry: ghcr.io }
      - run: docker build -t ghcr.io/${{ github.actor }}/transaction-service:latest ./transaction-service
      - run: docker build -t ghcr.io/${{ github.actor }}/fraud-service:latest ./fraud-service
      - run: docker push ghcr.io/${{ github.actor }}/transaction-service:latest
      - run: docker push ghcr.io/${{ github.actor }}/fraud-service:latest
```

Jenkins equivalent talking point: parallel test-java and test-python are Jenkins parallel stages. The needs: dependency is Jenkins stage gating. I can describe this pipeline in either tool.

---

# 13. Cloud Deployment Plan

**Quickest live demo: Railway**
```bash
railway init && railway up
# Add MongoDB Atlas M0 free cluster separately
```

**Production-grade AWS:**

| Local | AWS Equivalent |
|---|---|
| kafka + zookeeper | Amazon MSK (Managed Streaming for Kafka) |
| postgres | Amazon RDS PostgreSQL Multi-AZ |
| redis | Amazon ElastiCache |
| mongodb | MongoDB Atlas M10 or AWS DocumentDB |
| transaction-service | ECS Fargate Task A (scale on CPU) |
| fraud-service | ECS Fargate Task B (scale on Kafka consumer lag metric) |
| frontend | S3 + CloudFront |

Key AWS interview insight: The fraud service is stateless — it pulls rules from MongoDB and publishes to Kafka. On MSK I would configure the Kafka consumer group so adding more Fargate tasks automatically shares partition load. Scale the fraud service independently when transaction volume spikes without touching the Transaction Service.

---

# 14. Interview Talking Points

**Why Kafka / why async**
- "In production banking, fraud detection runs async — you cannot add 200ms of fraud check latency to every payment response. The Transaction Service publishes an event and returns 202 immediately. The Fraud Service consumes at its own pace. If the fraud service is temporarily down, Kafka buffers the events — no data is lost and the payment API stays responsive. I saw this exact pattern at JP Morgan."

**Why two stacks / why Python**
- "Java is the right choice for transactional systems — strong typing, mature ORM, Spring ecosystem. Python dominates analytical and risk work — faster iteration on rule logic, and it is what most risk teams actually use. Building both services in this project shows I can choose the right tool for each job and communicate across language boundaries through event schemas."

**Polyglot persistence**
- "The Transaction Service owns a PostgreSQL database and a MongoDB audit log. The Fraud Service owns a separate MongoDB database for fraud rules and assessments. Neither service touches the other's data store. The only exchange is the Kafka event schema. This is the database-per-service pattern that enables independent deployability."

**Idempotency + Kafka at-least-once delivery**
- "Kafka guarantees at-least-once delivery — the same event can be delivered more than once. If the Fraud Service crashes after evaluating but before acknowledging the message, it will re-consume the event. My fraud engine checks MongoDB before evaluating: if an assessment for that transactionId already exists, it re-publishes the existing result. The consumer is idempotent."

**@Transactional and Kafka publish ordering**
- "One subtle design issue: if I publish the Kafka event inside the @Transactional block and the DB commit fails after, the event is already published but the transaction record does not exist. I solved this by publishing the Kafka event only after the @Transactional method returns successfully — the event fires if and only if the DB write committed."

**State machine and CS fundamentals**
- "Transaction status transitions are a directed graph. I keep a Map<TransactionStatus, Set<TransactionStatus>> of valid edges. PENDING_FRAUD_CHECK can move to PROCESSING or FRAUD_REJECTED. COMPLETED can only move to REVERSED. Invalid transition returns 409. This directly applies the graph data structures the JD lists as required CS fundamentals."

**JP Morgan connection**
- "At JP Morgan, payments flow through multiple downstream systems — risk, compliance, settlement, notification — all decoupled by message queues. This project replicates that pattern: Transaction Service is the payment core, Fraud Service is the risk layer, Kafka is the message backbone. I built it to stay sharp on the architecture I worked with, and to explain it clearly in an interview."

**Demonstrating independence and quick productivity**
- "I built this in two stacks — Java and Python — to demonstrate I can be productive in either context. The architecture is the same pattern used at real banks. If FPT places me on a team that uses Java for core services and Python for analytics, I can contribute to both without a ramp-up period."
