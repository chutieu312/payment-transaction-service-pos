# Project Skills Doc — Payment Transaction Service

This is the living inventory of the technologies, patterns, and workflow skills currently implemented in this project. Update this file whenever new skills or tech are added to the codebase.

## Backend Skills

- Java 21
- Spring Boot 3.3
- Spring Security
- Spring Data JPA
- Spring Data MongoDB
- Spring Data Redis
- Spring Kafka
- Flyway
- JJWT
- springdoc-openapi / Swagger UI
- Lombok
- Spring Actuator

## Frontend Skills

- React 18
- TypeScript 5.4
- React Router 6
- Vite
- Axios
- Tailwind CSS 3.4
- Storybook

## Database Skills

- PostgreSQL 16
- MongoDB 7
- Redis 7

## Messaging Skills

- Apache Kafka 7.6.1
- Kafka producer / consumer event flow
- Zookeeper for local Kafka coordination

## Testing Skills

- JUnit 5
- Mockito
- MockMvc
- Spring Boot Test
- spring-kafka-test
- spring-security-test
- Testcontainers
- pytest
- pytest-asyncio
- pytest-mock
- httpx test client

## DevOps / CI-CD Skills

- GitHub Actions
- Docker
- Docker Compose
- Multi-stage Docker builds
- GitHub Container Registry (GHCR)

## Security Skills

- JWT bearer authentication
- BCrypt password hashing
- Role-based access control (CUSTOMER / BANK_ADMIN)
- Stateless API security
- Idempotency keys for duplicate request protection

## Observability / DX Skills

- SLF4J logging
- Spring Actuator health endpoints
- FastAPI automatic OpenAPI docs
- Kafka UI
- Adminer
- Mongo Express
- Swagger UI

## Current Project Features That Demonstrate These Skills

- JWT login and protected routes
- Async fund transfer flow with 202 Accepted response
- Kafka-driven fraud assessment pipeline
- Transaction state machine with valid transition rules
- Redis-backed balance caching
- PostgreSQL persistence with Flyway migrations
- MongoDB audit trail and fraud assessment storage
- React dashboard, transfer form, history view, and admin panel
- GitHub Actions pipeline with unit, frontend, and integration stages

## Maintenance Rule

After any approved change that introduces a new framework, library, infrastructure tool, testing tool, or architecture pattern, add it to this file before finishing the workflow.