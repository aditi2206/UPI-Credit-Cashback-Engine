# Kiwi Credit & Cashback Engine

A production-grade backend system simulating the core of a UPI credit card fintech — credit limit management, double-entry ledger accounting, and a configurable cashback rules engine.

Built to demonstrate the real engineering patterns used by companies like Kiwi (gokiwi.in).

---

## Architecture

```mermaid
flowchart TD
    subgraph API["🌐 API Layer"]
        A1["POST /payments\nMake a payment"]
        A2["GET /wallet\nWallet balance"]
        A3["GET /credit\nAvailable limit"]
        A4["GET /transactions\nHistory"]
    end

    subgraph SVC["⚙️ Service Layer"]
        S1["Payment Service\n1. Check idempotency key\n2. Validate credit limit\n3. Write ledger entries\n4. Update txn status"]
        S2["Rules Engine\nPicks cashback %"]
        S3["Cashback Calculator\nApplies monthly cap"]
    end

    subgraph LEDGER["📒 Double-Entry Ledger"]
        L1["Ledger Entries\nCASHBACK_POOL → DEBIT ₹50\nUSER_WALLET  → CREDIT ₹50\nCREDIT_LIMIT → DEBIT ₹1000"]
        L2["Audit Event Log\nPAYMENT_CREATED\nCASHBACK_APPLIED\nLIMIT_UPDATED"]
    end

    subgraph DB["🗄️ MySQL"]
        D1[(users)]
        D2[(transactions)]
        D3[(ledger_entries)]
        D4[(cashback_rules)]
        D5[(event_log)]
    end

    subgraph CC["🛡️ Cross-Cutting Concerns"]
        C1["Idempotency\nDedup on key"]
        C2["Concurrency\nOptimistic locking"]
        C3["Rate Limiting\nBucket4j"]
        C4["Observability\nSpring Actuator"]
    end

    A1 --> S1
    A2 --> D3
    A3 --> D1
    A4 --> D2

    S1 --> S2
    S2 --> S3
    S1 --> L1
    S1 --> L2

    L1 --> D1
    L1 --> D2
    L1 --> D3
    L2 --> D5
    S2 --> D4

    S1 -.->|guards| C1
    S1 -.->|uses| C2
    A1 -.->|throttled by| C3
    S1 -.->|emits metrics| C4

    style API fill:#EEEDFE,stroke:#534AB7,color:#26215C
    style SVC fill:#E1F5EE,stroke:#0F6E56,color:#04342C
    style LEDGER fill:#FAECE7,stroke:#993C1D,color:#4A1B0C
    style DB fill:#F1EFE8,stroke:#5F5E5A,color:#2C2C2A
    style CC fill:#E6F1FB,stroke:#185FA5,color:#042C53
```

---

## Production-grade Features

| Feature | Implementation |
|---|---|
| **Double-entry ledger** | Every rupee movement creates two ledger rows — balances are derived, never stored |
| **Idempotency** | Unique key per request; duplicate requests return the original response, never double-charge |
| **Optimistic locking** | `@Version` on User entity prevents concurrent payments causing incorrect credit deductions |
| **Configurable rules engine** | Cashback % and monthly caps live in DB — change rates without redeploying |
| **Monthly cap enforcement** | Queries ledger entries to check how much cashback already earned this month |
| **Audit event log** | Immutable record of every system event with before/after snapshots |
| **Rate limiting** | Bucket4j in-memory rate limiter on the payments endpoint (10 req/min) |
| **Observability** | Spring Actuator exposes `/actuator/metrics`, `/actuator/health` |
| **Concurrency test** | 50 simultaneous payment threads — proves no lost updates via assertions |

---

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2, Spring Data JPA
- **Database:** MySQL 8
- **Rate limiting:** Bucket4j
- **Observability:** Spring Actuator
- **Frontend:** Vanilla HTML/CSS/JS (demo only)
- **Containerisation:** Docker + Docker Compose

---

## Database Schema

### `users`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR | |
| email | VARCHAR UNIQUE | |
| credit_limit | DECIMAL(12,2) | Total card limit |
| used_credit | DECIMAL(12,2) | Amount consumed so far |
| version | BIGINT | Optimistic lock version |

### `transactions`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| user_id | FK → users | |
| amount | DECIMAL(12,2) | |
| category | ENUM | GROCERY, FOOD, FUEL, TRAVEL… |
| payment_mode | ENUM | UPI_SCAN, UPI_ONLINE, ONLINE |
| status | ENUM | PENDING → SUCCESS / FAILED |
| cashback_earned | DECIMAL(12,2) | |
| idempotency_key | VARCHAR(64) UNIQUE | Client-supplied dedup key |

### `ledger_entries`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| transaction_id | FK → transactions | |
| account_type | ENUM | USER_WALLET, CASHBACK_POOL, CREDIT_LIMIT |
| account_ref | VARCHAR | e.g. USER_1, SYSTEM |
| entry_type | ENUM | DEBIT / CREDIT |
| amount | DECIMAL(12,2) | |

### `cashback_rules`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| category | ENUM | |
| payment_mode | ENUM | |
| percentage | DECIMAL(5,2) | e.g. 5.00 = 5% |
| monthly_cap | DECIMAL(10,2) | Max cashback per user/month |
| valid_from / valid_to | DATETIME | Rule validity window |

---

## Running Locally

### Option 1 — Docker (one command)
```bash
docker-compose up --build
```
App runs on `http://localhost:8080`, MySQL on `3306`.

### Option 2 — Manual
```bash
# Start MySQL
mysql -u root -p
CREATE DATABASE kiwi_engine;

# Run Spring Boot
cd backend
mvn spring-boot:run
```

### Frontend
Open `frontend/index.html` directly in your browser (no server needed).

---

## API Reference

### Make a payment
```
POST /api/payments
Content-Type: application/json

{
  "userId": 1,
  "amount": 1000,
  "category": "GROCERY",
  "paymentMode": "UPI_SCAN",
  "idempotencyKey": "unique-client-key-123"
}
```

**Response:**
```json
{
  "transactionId": 42,
  "amount": 1000.00,
  "cashbackEarned": 50.00,
  "walletBalance": 250.00,
  "availableCredit": 49000.00,
  "status": "SUCCESS",
  "idempotencyHit": false,
  "message": "Payment successful! ₹50.00 cashback earned."
}
```

### Get user summary
```
GET /api/users/{userId}
```

### Get ledger entries
```
GET /api/payments/ledger/{userId}
```

### Get audit log
```
GET /api/payments/audit/{userId}
```

### Health check
```
GET /actuator/health
```

---

## Cashback Rules (seeded on startup)

| Payment Mode | Cashback | Monthly Cap |
|---|---|---|
| UPI Scan-and-Pay | 5% | ₹500 |
| UPI Online | 2% | ₹300 |
| Online / Card | 1.5% | ₹200 |

Rules are stored in the `cashback_rules` table and can be updated without redeploying.

---

## Design Decisions

### Why double-entry ledger instead of a balance column?
A `wallet_balance` column updated via `UPDATE` is vulnerable to race conditions and gives no audit history. The double-entry approach inserts immutable rows — balances are derived by querying the ledger, which means they are self-verifying (all debits + credits across all accounts should sum to zero) and fully auditable.

### Why optimistic locking?
Credit limit deduction requires a read → check → write sequence. Without locking, two concurrent payments can both read the same balance, both pass the limit check, and both deduct — causing overdraft. Optimistic locking via `@Version` makes one of them fail with an `ObjectOptimisticLockingFailureException`, which Spring retries safely.

### Why idempotency keys?
UPI payments are retried by clients on network timeouts. Without deduplication, a timeout followed by a retry could double-charge the user. The client sends a unique key per payment intent; the server stores it and returns the original response on duplicates.

### Why a DB-driven rules engine?
Hardcoded `if-else` cashback logic means every promotional rate change requires a code deployment. Storing rules in `cashback_rules` with validity windows lets you update rates, launch limited-time promos, and A/B test cashback percentages via a simple DB update.

---

## Running the Concurrency Test
```bash
cd backend
mvn test -Dtest=ConcurrencyTest
```
Fires 50 simultaneous payment threads and asserts the final wallet balance and used credit are mathematically exact — proving no lost updates.
