# 📒 Ledger — Event-Sourced Banking Microservice

> A production-grade, event-sourced banking ledger built with **Spring Boot 4**, **Apache Kafka**, and **PostgreSQL** — implementing **CQRS**, **Outbox Pattern**, **Idempotency**, **Snapshotting**, and **Resilience4j Circuit Breaking**.

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img src="https://img.shields.io/badge/Apache_Kafka-3.7.0-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-336791?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white" />
</p>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Key Design Patterns](#-key-design-patterns)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [API Reference](#-api-reference)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [Database Migrations](#-database-migrations)
- [Observability](#-observability)
- [Resilience](#-resilience)

---

## 🌟 Overview

**Ledger** is a high-integrity, event-sourced financial microservice that models bank accounts as immutable **event streams**. Every state change (account opening, deposit, withdrawal) is captured as a domain event — making the system fully auditable, temporally queryable, and replay-safe.

The system is built around three core guarantees:

| Guarantee | Implementation |
|---|---|
| **Exactly-once command processing** | Idempotency filter with database-backed key store |
| **Reliable event propagation** | Transactional Outbox Pattern + Kafka |
| **Fast aggregate reconstruction** | Snapshotting (every 5 events) |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         REST API Layer                              │
│          POST /api/accounts     POST /api/accounts/{id}/deposit     │
│          POST /api/accounts/{id}/withdraw   GET /api/accounts/{id}  │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
           ┌───────────────▼───────────────┐
           │       COMMAND SIDE (Write)     │
           │  AccountCommandService         │
           │  • Load aggregate from ES      │
           │  • Execute command             │
           │  • Persist events + outbox     │
           │    (single transaction)        │
           └───────────────┬───────────────┘
                           │  PostgreSQL
           ┌───────────────▼───────────────┐
           │        EVENT STORE             │
           │  event_store   (append-only)   │
           │  account_snapshots             │
           │  outbox_entries                │
           │  idempotency_keys              │
           └───────────────┬───────────────┘
                           │
           ┌───────────────▼───────────────┐
           │      OUTBOX PUBLISHER          │
           │  Polls outbox table            │
           │  Publishes to Kafka            │
           │  Marks entries published       │
           └───────────────┬───────────────┘
                           │  Apache Kafka
           ┌───────────────▼───────────────┐
           │       QUERY SIDE (Read)        │
           │  AccountProjection (consumer)  │
           │  Maintains account_read_model  │
           │  Serves GET /api/accounts      │
           └───────────────────────────────┘
```

### Event Flow

```
Command → Validate → Raise Domain Event → Persist to EventStore
       → Write to Outbox (same TX) → OutboxPublisher → Kafka
       → AccountProjection Consumer → Update Read Model
```

---

## 🎯 Key Design Patterns

### 1. Event Sourcing
The `Account` aggregate **never stores current state directly**. Instead, it reconstructs itself by replaying its event history. The `Account.replay()` factory method loads all events from `event_store` and applies them sequentially.

```java
// Aggregate rebuilt purely from event history
Account account = Account.replay(history);

// Or fast-path via snapshot + delta events
Account account = Account.restoreFromSnapshot(snapshot, eventsSinceSnapshot);
```

### 2. CQRS (Command Query Responsibility Segregation)
- **Command Side**: `AccountCommandService` handles writes. Returns `202 Accepted` — writes are asynchronous.
- **Query Side**: `AccountQueryController` reads from a denormalized `account_read_model` table updated by Kafka consumers.
- Both sides are independently evolvable and deployable.

### 3. Transactional Outbox Pattern
Commands persist domain events AND outbox entries within a **single database transaction**, guaranteeing atomicity. The `OutboxPublisher` scheduler then polls and delivers entries to Kafka — eliminating dual-write inconsistency.

```
┌─── Single ACID Transaction ────────────────┐
│  INSERT INTO event_store (...)             │
│  INSERT INTO outbox_entries (...)          │
└────────────────────────────────────────────┘
         │ (async, scheduled)
         ▼
    Kafka Topic: account-events
```

### 4. Idempotency
Every mutating HTTP request must include an `Idempotency-Key` header. The `IdempotencyFilter` checks for duplicate keys before the command reaches the service layer — preventing duplicate operations from retried or replayed requests.

### 5. Snapshotting
To avoid replaying the entire event history on every request, the system automatically takes a **snapshot every 5 events**. Subsequent loads use `Account.restoreFromSnapshot()` + only the delta events since the last snapshot.

### 6. Resilience4j Circuit Breaker
Kafka publishing is wrapped with a circuit breaker (`kafkaPublish` instance). If Kafka becomes unavailable, the breaker opens and events remain safely in the outbox until Kafka recovers — providing graceful degradation.

---

## 🛠️ Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Runtime** | Java 17 | Language runtime |
| **Framework** | Spring Boot 4.1.0 | Application framework |
| **Persistence** | Spring Data JPA + PostgreSQL 16 | Event store & read models |
| **Migrations** | Flyway | Schema version control |
| **Messaging** | Apache Kafka 3.7.0 (KRaft) | Event streaming |
| **Resilience** | Resilience4j 2.2.0 | Circuit breaker for Kafka |
| **API Docs** | SpringDoc OpenAPI 2.5.0 | Swagger UI |
| **Observability** | Micrometer + Prometheus + Grafana | Metrics & dashboards |
| **Build** | Maven | Dependency & build management |
| **Containerization** | Docker + Docker Compose | Infrastructure orchestration |
| **Boilerplate** | Lombok | Reduce Java verbosity |

---

## 📁 Project Structure

```
ledger/
├── src/main/java/com/sudesh/ledger/
│   ├── LedgerApplication.java              # Spring Boot entry point
│   │
│   ├── command/                            # WRITE SIDE
│   │   ├── api/
│   │   │   ├── AccountController.java      # POST endpoints (open/deposit/withdraw)
│   │   │   └── dto/                        # Request DTOs with Bean Validation
│   │   ├── domain/
│   │   │   ├── Account.java                # Aggregate root (event-sourced)
│   │   │   ├── AccountStatus.java          # OPEN enum
│   │   │   ├── AccountEventCodec.java      # Serialize/deserialize domain events
│   │   │   ├── command/                    # OpenAccountCommand, DepositCommand, etc.
│   │   │   ├── event/                      # AccountOpened, MoneyDeposited, etc.
│   │   │   └── exception/                  # InsufficientFundsException
│   │   └── service/
│   │       └── AccountCommandService.java  # Orchestrates load → execute → persist
│   │
│   ├── query/                              # READ SIDE
│   │   ├── api/
│   │   │   ├── AccountQueryController.java # GET endpoints
│   │   │   └── ProjectionAdminController  # Replay/rebuild projections
│   │   ├── projection/                     # Kafka consumers, updates read model
│   │   └── repository/                     # Read model JPA repositories
│   │
│   ├── eventstore/                         # CORE INFRASTRUCTURE
│   │   ├── EventStore.java                 # Append & load event streams
│   │   ├── EventStoreRepository.java       # JPA for event_store table
│   │   ├── StoredEvent.java                # JPA entity for persisted events
│   │   ├── SnapshotStore.java              # Save & load account snapshots
│   │   ├── AccountSnapshot.java            # JPA entity for snapshots
│   │   ├── AccountSnapshotRepository.java  # JPA for account_snapshots table
│   │   ├── OutboxEntry.java                # JPA entity for outbox_entries
│   │   ├── OutboxRepository.java           # JPA for outbox table
│   │   └── OutboxPublisher.java            # Scheduled: poll outbox → Kafka
│   │
│   └── shared/                             # CROSS-CUTTING CONCERNS
│       ├── event/
│       │   ├── DomainEventEnvelope.java    # Typed Kafka message wrapper
│       │   └── DomainEventEnvelopeSerde.java
│       ├── idempotency/
│       │   ├── IdempotencyFilter.java      # Servlet filter for dedup
│       │   ├── IdempotencyService.java     # Key check & registration logic
│       │   ├── IdempotencyKeyEntity.java   # JPA entity
│       │   └── IdempotencyKeyRepository.java
│       ├── exception/
│       │   └── IdempotencyKeyConflictException.java
│       ├── error/                          # Global exception handler
│       └── metrics/
│           └── LedgerMetrics.java          # Custom Micrometer counters
│
├── src/main/resources/
│   ├── application.yml                     # App configuration
│   └── db/migration/                       # Flyway scripts
│       ├── V1__create_event_store.sql
│       ├── V2__create_read_models.sql
│       ├── V3__create_snapshots.sql
│       ├── V4__create_idempotency_keys.sql
│       ├── V5__create_outbox.sql
│       └── V6__add_outbox_retry_columns.sql
│
├── Dockerfile                              # Multi-stage build
├── docker-compose.yml                      # Full infrastructure stack
└── prometheus.yml                          # Prometheus scrape config
```

---

## 🌐 API Reference

### Command Endpoints (Write Side)

> All write endpoints require an `Idempotency-Key` header and return `202 Accepted`.

#### Open Account
```http
POST /api/accounts
Idempotency-Key: <uuid>
Content-Type: application/json

{
  "accountId": "acc-001",
  "ownerName": "Sudesh",
  "openingBalance": 1000.00
}
```

#### Deposit Money
```http
POST /api/accounts/{accountId}/deposit
Idempotency-Key: <uuid>
Content-Type: application/json

{
  "amount": 500.00,
  "reference": "salary-aug-2026"
}
```

#### Withdraw Money
```http
POST /api/accounts/{accountId}/withdraw
Idempotency-Key: <uuid>
Content-Type: application/json

{
  "amount": 250.00,
  "reference": "rent-sep-2026"
}
```

### Query Endpoints (Read Side)

#### Get Account
```http
GET /api/accounts/{accountId}
```

#### List All Accounts
```http
GET /api/accounts
```

### Interactive Docs
Once running, visit **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)** for the full interactive Swagger UI.

---

## 🚀 Getting Started

### Prerequisites

| Tool | Minimum Version |
|---|---|
| Java | 17 |
| Maven | 3.9+ |
| Docker | 24+ |
| Docker Compose | v2 |

### 1. Clone the Repository

```bash
git clone https://github.com/Sudesh-2002/ledger.git
cd ledger
```

### 2. Start Infrastructure

Spin up PostgreSQL, Kafka (KRaft mode), Kafka UI, Prometheus, and Grafana with a single command:

```bash
docker compose up -d postgres kafka kafka-ui prometheus grafana
```

Wait for all services to become healthy (usually ~15 seconds).

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The application will:
- Connect to PostgreSQL at `localhost:5432`
- Run Flyway migrations automatically (V1 → V6)
- Connect to Kafka at `localhost:9092`
- Expose the REST API at `http://localhost:8080`

### 4. Try It Out

```bash
# 1. Open a new account
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"accountId": "acc-001", "ownerName": "Sudesh", "openingBalance": 1000.00}'

# 2. Make a deposit
curl -X POST http://localhost:8080/api/accounts/acc-001/deposit \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"amount": 500.00, "reference": "salary"}'

# 3. Query the account (read model)
curl http://localhost:8080/api/accounts/acc-001
```

---

## ⚙️ Configuration

All configuration lives in `src/main/resources/application.yml`. Key properties:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ledger
    username: ledger
    password: ledger
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ledger-projector

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics

resilience4j:
  circuitbreaker:
    instances:
      kafkaPublish:
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

### Environment Variable Overrides (Docker)

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/ledger` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `ledger` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `ledger` | DB password |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker(s) |

---

## 🗄️ Database Migrations

Schema is managed by **Flyway** and applied automatically on startup:

| Version | Script | Description |
|---|---|---|
| V1 | `V1__create_event_store.sql` | `event_store` table (append-only event log) |
| V2 | `V2__create_read_models.sql` | `account_read_model` table (query side) |
| V3 | `V3__create_snapshots.sql` | `account_snapshots` table |
| V4 | `V4__create_idempotency_keys.sql` | `idempotency_keys` table |
| V5 | `V5__create_outbox.sql` | `outbox_entries` table |
| V6 | `V6__add_outbox_retry_columns.sql` | Retry count & last-attempt timestamp |

---

## 📊 Observability

The full observability stack is included in `docker-compose.yml`:

| Service | URL | Credentials |
|---|---|---|
| **Application** | http://localhost:8080 | — |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | — |
| **Kafka UI** | http://localhost:8081 | — |
| **Prometheus** | http://localhost:9090 | — |
| **Grafana** | http://localhost:3000 | admin / admin |

### Custom Metrics (via `LedgerMetrics.java`)

The application exposes custom Micrometer counters scraped by Prometheus:

- `ledger.commands.processed` — total commands handled
- `ledger.events.published` — events successfully sent to Kafka
- `ledger.idempotency.duplicates` — duplicate requests rejected
- `ledger.snapshots.created` — snapshots taken

Access raw metrics at: `GET http://localhost:8080/actuator/prometheus`

---

## 🛡️ Resilience

### Circuit Breaker — Kafka Publishing

The `OutboxPublisher` wraps Kafka sends with the `kafkaPublish` Resilience4j circuit breaker:

```
CLOSED → (50% failure rate over 20 calls) → OPEN (10s) → HALF_OPEN (5 test calls) → CLOSED
```

When **OPEN**: Kafka publishing is skipped. Events remain safely in the `outbox_entries` table and are retried when the breaker transitions to `HALF_OPEN`.

### Idempotency — Duplicate Request Protection

```
Request arrives → IdempotencyFilter checks DB for key
  ├── Key exists → 409 Conflict (or cached response)
  └── Key not found → Proceed → Register key after success
```

### Snapshotting — Fast Aggregate Load

```
Load Account:
  1. Find latest snapshot for accountId
  2. Load only events AFTER snapshot.version
  3. Account.restoreFromSnapshot(snapshot, deltaEvents)

  (Fallback: Account.replay(allEvents) if no snapshot exists)
```

---

## 🏃 Running with Full Docker Stack

To run the entire application including the app container:

```bash
docker compose up --build
```

> **Note**: The `app` service in `docker-compose.yml` builds from the local `Dockerfile` using a multi-stage Maven build. Ensure Docker has sufficient memory (≥4 GB) for the Maven build stage.

---

## 📄 License

This project is for educational and portfolio purposes. Feel free to reference, fork, or build upon it.

---

<p align="center">
  Built with ❤️ by <strong>Sudesh</strong> · Event Sourcing · CQRS · Kafka · Spring Boot
</p>
