# Session 06 - Exercise 04 - Integration Test Report

Date: 2026-11-16
Workspace: `/home/white_ember/DevOps-Lab/md05_ss06_bai_04`
Compose project: `rikkei-ss06-banking01`

## 1. Environment

| Item | Value |
|---|---|
| Java | OpenJDK 21.0.12 (host); Temurin 21.0.13 in the container |
| Spring Boot | 4.1.0 |
| Gradle wrapper | 9.5.1 |
| Compose | Docker Compose 5.1.4 |
| PostgreSQL | 17.11 (`postgres:17-alpine`) |
| Docker network | `banking-network` (bridge) |
| Database | `banking_db` |
| Volume | `rikkei-ss06-banking01_banking-db-data` |

## 2. Pre-deployment baseline

The existing database volume was inspected before deployment. The existing
accounts table contained:

```text
 id |       account_number       |  owner_name  |  balance   |          created_at
----+----------------------------+--------------+------------+-------------------------------
  1 | ACC-E02-API-20261115171904 | Nguyen Van A | 1000000.00 | 2026-11-15 17:19:05.743842+00
  2 | ACC-E02-API-20261115172152 | Nguyen Van A | 1000000.00 | 2026-11-15 17:21:52.938821+00
  3 | ACC-E02-PM-20261115-01     | Nguyen Van A | 1000000.00 | 2026-11-15 19:14:32.357689+00
```

```text
 account_count
---------------
             3
```

The volume was reused as-is. `docker compose down -v` was never run.

## 3. Static configuration validation

```bash
docker compose config -q
```

Result: exit code 0.

```text
Services: core-db, account-service, transaction-service
Network:  banking-network (bridge)
Volume:   pg-data -> external rikkei-ss06-banking01_banking-db-data
Ports:    account-service 8080:8080, transaction-service 8081:8081
          core-db internal 5432 only (see host-port note)
```

`core-db` is internal-only because the host NixOS `postgresql.service` already
owns `0.0.0.0:5432`. This deviation was explicitly approved by the user.

## 4. Deployment

```bash
docker compose up -d --build
docker compose ps
```

```text
rikkei-ss06-banking01-account-service-1      Up (healthy)  8080->8080
rikkei-ss06-banking01-core-db-1              Up (healthy)  5432/tcp
rikkei-ss06-banking01-transaction-service-1  Up (healthy)  8081->8081
```

Startup evidence:

```text
transaction-service: Tomcat started on port 8081
account-service:     HikariPool initialized, accounts table verified
core-db:             database already initialized, volume reused
```

No restart loop was observed.

## 5. Test matrix

All requests were made against `http://localhost:8081`.

### Test A - Existing source account

```bash
curl -sS -i -X POST http://localhost:8081/api/transactions/transfer \
  -H 'Content-Type: application/json' \
  -H 'X-Request-ID: test-A-evidence' \
  -d '{"sourceAccountId":3,"destinationAccountId":2,"amount":100000}'
```

```http
HTTP/1.1 200
X-Request-ID: test-A-evidence
Content-Type: application/json

{"sourceAccountId":3,"destinationAccountId":2,"amount":100000,"status":"SOURCE_ACCOUNT_VERIFIED","message":"Source account exists. Transfer execution is not implemented."}
```

PASS.

### Test B - Unknown source account

```bash
curl ... -d '{"sourceAccountId":999999,"destinationAccountId":2,"amount":100000}'
```

```http
HTTP/1.1 404

{"status":404,"error":"Not Found","message":"Source account not found: 999999",...}
```

PASS.

### Test C - Negative amount

```json
{"sourceAccountId":3,"destinationAccountId":2,"amount":-5}
```

```http
HTTP/1.1 400

{"status":400,"error":"Bad Request","message":"amount: amount must be greater than zero",...}
```

PASS.

### Test D - Zero amount

```json
{"sourceAccountId":3,"destinationAccountId":2,"amount":0}
```

```http
HTTP/1.1 400
```

PASS.

### Test E - Same source and destination

```json
{"sourceAccountId":3,"destinationAccountId":3,"amount":100000}
```

```http
HTTP/1.1 400

{"status":400,"error":"Bad Request","message":"sourceAccountId and destinationAccountId must differ",...}
```

PASS.

### Test F - Missing required fields

```json
{"sourceAccountId":3}
```

```http
HTTP/1.1 400

{"status":400,"error":"Bad Request","message":"destinationAccountId: destinationAccountId is required; amount: amount is required",...}
```

A negative source account ID also returned 400:

```text
sourceAccountId: sourceAccountId must be positive
```

PASS.

### Test G - account-service unavailable

The project-scoped `account-service` container was stopped temporarily:

```bash
docker stop rikkei-ss06-banking01-account-service-1
curl -sS -i -X POST http://localhost:8081/api/transactions/transfer \
  -H 'Content-Type: application/json' \
  -H 'X-Request-ID: test-G-upstream-down' \
  -d '{"sourceAccountId":3,"destinationAccountId":2,"amount":100000}'
```

```http
HTTP/1.1 503

{"status":503,"error":"Service Unavailable","message":"account-service is unavailable","path":"/api/transactions/transfer","requestId":"test-G-upstream-down"}
```

The container was restarted, became healthy, and Test A returned 200 again.

PASS.

## 6. Docker DNS verification

Containers on `banking-network`:

```text
rikkei-ss06-banking01-core-db-1              172.29.0.2/16
rikkei-ss06-banking01-account-service-1      172.29.0.3/16
rikkei-ss06-banking01-transaction-service-1  172.29.0.4/16
```

DNS resolution from inside `transaction-service`:

```bash
docker exec rikkei-ss06-banking01-transaction-service-1 getent hosts account-service
```

```text
172.29.0.3        account-service  account-service
```

Internal HTTP request from `transaction-service`:

```bash
docker exec rikkei-ss06-banking01-transaction-service-1 \
  wget -qO- http://account-service:8080/api/accounts/3
```

```json
{"id":3,"accountNumber":"ACC-E02-PM-20261115-01","ownerName":"Nguyen Van A","balance":1000000.00,"createdAt":"2026-11-15T19:14:32.357689Z"}
```

The application configuration and source contain no `localhost`, `127.0.0.1`,
host IP, or container IP for the inter-service call. The only `localhost` in the
Compose file is inside the container healthchecks.

## 7. Cross-service correlation logs

The screenshot `inter_service_logs.png` shows a live request with correlation ID
`evidence-live-1794801288`:

```text
transaction-service: Received POST /api/transactions/transfer
transaction-service: Received transfer validation request: sourceAccountId=3, destinationAccountId=2, amount=100000
transaction-service: Calling http://account-service:8080/api/accounts/3 via Docker internal DNS
account-service:     Received GET /api/accounts/3
account-service:     Account found: id=3
transaction-service: account-service returned HTTP 200
transaction-service: Source account verified: id=3, accountNumber=ACC-E02-PM-20261115-01, owner=Nguyen Van A
account-service:     Completed GET /api/accounts/3 -> HTTP 200
transaction-service: Completed POST /api/transactions/transfer -> HTTP 200
```

## 8. Database integrity after testing

```text
 id |       account_number       |  owner_name  |  balance   |          created_at
----+----------------------------+--------------+------------+-------------------------------
  1 | ACC-E02-API-20261115171904 | Nguyen Van A | 1000000.00 | 2026-11-15 17:19:05.743842+00
  2 | ACC-E02-API-20261115172152 | Nguyen Van A | 1000000.00 | 2026-11-15 17:21:52.938821+00
  3 | ACC-E02-PM-20261115-01     | Nguyen Van A | 1000000.00 | 2026-11-15 19:14:32.357689+00
```

```text
 account_count | total_balance
---------------+---------------
             3 |    3000000.00
```

No account was created, deleted, or modified. No debit/credit operation is
implemented.

## 9. Evidence validation

| File | Dimensions | SHA-256 |
|---|---|---|
| `inter_service_code.png` | 2560x1440 | `a63a24b5fa4b2bb711b4b089f2d1acfbacfd29b925cc06a7ed6621006d840cbb` |
| `inter_service_logs.png` | 2560x1440 | `600f6e776e216724501d7d3bbb77a749c37a5372a7da3be372dca89f17412884` |

Both files have a valid PNG signature and were captured from real Foot
terminal windows using `grim`.
