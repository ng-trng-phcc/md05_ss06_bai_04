# Rikkei DevOps - Session 06 - Exercise 04

Banking transaction service with Docker internal DNS and inter-service HTTP
communication.

## Objective

Extend the existing Banking stack with a `transaction-service` that validates a
transfer request by calling `account-service` through Docker's internal DNS.
The exercise validates the source account only. It does **not** debit or credit
any balance.

## Architecture

```text
                     HOST
                       |
                       | HTTP :8081
                       v
             +---------------------+
             | transaction-service |
             | Spring Boot 4.1.0   |
             | Port 8081           |
             +----------+----------+
                        |
                        | GET /api/accounts/{id}
                        | http://account-service:8080
                        | Docker internal DNS
                        v
             +---------------------+
             | account-service     |
             | Spring Boot 4.1.0   |
             | Port 8080           |
             +----------+----------+
                        |
                        | JDBC core-db:5432
                        v
             +---------------------+
             | core-db             |
             | PostgreSQL 17       |
             | Database banking_db |
             +---------------------+

             Docker network: banking-network (bridge)
             Compose project: rikkei-ss06-banking01
```

## Services

| Service | Image | Host port | Container port | Network |
|---|---|---:|---:|---|
| `core-db` | `postgres:17-alpine` | none (see note) | 5432 | `banking-network` |
| `account-service` | `account-service:latest` | 8080 | 8080 | `banking-network` |
| `transaction-service` | `transaction-service:latest` | 8081 | 8081 | `banking-network` |

The existing database is preserved through an external Compose volume:

```yaml
volumes:
  pg-data:
    external: true
    name: rikkei-ss06-banking01_banking-db-data
```

The previous Compose project identity (`rikkei-ss06-banking01`) is kept so the
same physical database volume is reused. No volume was deleted or recreated.

### Environment note: host port 5432

This machine runs the NixOS `postgresql.service`, which already listens on
`0.0.0.0:5432`. Publishing `core-db` as `5432:5432` would fail to bind and would
require stopping an unrelated host service. With explicit user approval,
`core-db` is kept internal to `banking-network` only. It is still reachable by
the services at `core-db:5432`; no host database port is required by the
exercise API.

## transaction-service

- Java 21, Spring Boot 4.1.0, Gradle 9.5.1 wrapper.
- Dependencies: Spring Web, Bean Validation, Actuator.
- HTTP client: `RestTemplate` with connect/read timeouts.
- Base URL from `ACCOUNT_SERVICE_BASE_URL`, default
  `http://account-service:8080`.
- Server port from `SERVER_PORT`, default `8081`.
- Correlation ID: `X-Request-ID` is accepted or generated, propagated to
  `account-service`, and included in logs and error responses.

Key files:

```text
transaction-service/src/main/java/com/rikkei/banking/transaction/
├── TransactionApplication.java
├── client/AccountServiceClient.java
├── config/RestTemplateConfig.java
├── controller/TransactionController.java
├── dto/AccountResponse.java
├── dto/TransferRequest.java
├── dto/TransferResponse.java
├── exception/...
├── service/TransactionService.java
└── web/RequestIdFilter.java
```

## API

### Transfer validation

```http
POST http://localhost:8081/api/transactions/transfer
Content-Type: application/json
X-Request-ID: optional-correlation-id
```

```json
{
  "sourceAccountId": 3,
  "destinationAccountId": 2,
  "amount": 100000
}
```

Successful response:

```json
{
  "sourceAccountId": 3,
  "destinationAccountId": 2,
  "amount": 100000,
  "status": "SOURCE_ACCOUNT_VERIFIED",
  "message": "Source account exists. Transfer execution is not implemented."
}
```

### Result mapping

| Condition | HTTP status |
|---|---:|
| Source account exists | 200 |
| Source account does not exist | 404 |
| Invalid request or same account | 400 |
| `account-service` unreachable | 503 |
| `account-service` timeout | 504 |
| Unexpected upstream failure | 502 |

The account is verified from the real `account-service` response. No successful
verification is hardcoded.

## Run

```bash
cp .env.example .env
# set POSTGRES_USER and POSTGRES_PASSWORD in .env

docker compose config -q
docker compose up -d --build
docker compose ps
```

Check logs:

```bash
docker compose logs -f transaction-service account-service
```

## Integration tests

Full commands and recorded outputs are in
[`docs/integration-test-report.md`](docs/integration-test-report.md).

Summary:

| Test | Request | Expected | Actual |
|---|---|---:|---:|
| A | Existing source account `3` | 200 | 200 |
| B | Unknown source account `999999` | 404 | 404 |
| C | Negative amount | 400 | 400 |
| D | Zero amount | 400 | 400 |
| E | Same source/destination | 400 | 400 |
| F | Missing required fields | 400 | 400 |
| G | `account-service` stopped temporarily | 503/504 | 503 |

Database integrity after testing: 3 accounts, total balance `3000000.00`, all
balances unchanged.

Docker DNS proof:

```text
docker exec transaction-service getent hosts account-service
172.29.0.3  account-service account-service

docker exec transaction-service wget -qO- http://account-service:8080/api/accounts/3
{"id":3,...,"balance":1000000.00}
```

The inter-service URL never uses `localhost`, `127.0.0.1`, a host IP, or a
container IP.

## Evidence

| File | SHA-256 |
|---|---|
| `homework/session_06/exercise_04/inter_service_code.png` | `a63a24b5fa4b2bb711b4b089f2d1acfbacfd29b925cc06a7ed6621006d840cbb` |
| `homework/session_06/exercise_04/inter_service_logs.png` | `600f6e776e216724501d7d3bbb77a749c37a5372a7da3be372dca89f17412884` |

Both PNG files are 2560x1440 and were captured from real Foot terminal windows
with `grim`.

## Repository

- Repository: <https://github.com/ng-trng-phcc/md05_ss06_bai_04>
- Branch: `main`
- Commit message: `16-11-26 | md05_ss06_bài_04`
- Release tag: `ss06-bai-04`
