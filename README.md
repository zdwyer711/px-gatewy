# Pnyx Gateway (`px-gateway`)

A Spring Boot API gateway and authentication server for the Pnyx ecosystem. Sits between the mobile app (`px-web-portal`) and the blockchain node (`px-ledger`), handling user identity, JWT sessions, MongoDB persistence, image storage, and real-time event bridging.

---

## Features

- **JWT Authentication** — stateless access tokens (10 min) + refresh token rotation (7 days), BCrypt password hashing
- **Role System** — `OWNER` and `SHOP` roles embedded in JWT; `ROLE_OWNER` / `ROLE_SHOP` Spring Security authorities
- **Vehicle Service Ledger** — register vehicles and log service entries on-chain with full MongoDB backing
- **Image Storage** — multipart photo upload with SHA-256 deduplication, stored in MongoDB (swappable to IPFS)
- **Blockchain Event Bridge** — SSE subscription to `px-ledger`; confirms `ServiceEntry` records `PENDING → CONFIRMED` when transactions are mined
- **WebSocket Broadcast** — pushes real-time balance/transaction events to connected clients via STOMP
- **Lock/Commit Proxy** — forwards signed 2PC transactions to `px-ledger`, passing `type` and `metadata` for vehicle/service transactions

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Core | Java 17+, Spring Boot 3.x |
| Database | MongoDB |
| Security | Spring Security, JJWT |
| Reactive | Spring WebFlux (WebClient for SSE), Spring WebSocket (STOMP) |
| Build | Maven |

---

## Architecture

```
px-web-portal  →  REST + JWT  →  px-gateway  →  REST (sync)  →  px-ledger
                                     ↕                              ↕
                              MongoDB (Users,                SSE stream
                              Vehicles, ServiceEntries,    (BLOCK_MINED)
                              Images, RefreshTokens)
```

**BlockEventProcessor** subscribes to px-ledger's SSE endpoint on startup. When a block is mined containing a `SERVICE_ENTRY` transaction, it looks up the matching `ServiceEntry` by `blockchainTxid` and flips status `PENDING → CONFIRMED`.

---

## API Reference

### Auth (`/api/auth`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/register` | No | `{ username, password, role? }` — role defaults to `OWNER` |
| POST | `/login` | No | Returns `{ accessToken, refreshToken, role, walletId }` |
| POST | `/refresh` | No | Exchange refresh token for new access token |

### Wallet Proxy (`/api/proxy`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/proxy/wallets` | Yes | Create ledger wallet; saves `walletId` on User |
| GET | `/proxy/{walletId}/nonce` | Yes | Current signing nonce |
| POST | `/proxy/lock` | Yes | 2PC Phase 1 — forward signed lock to px-ledger |
| POST | `/proxy/commit` | Yes | 2PC Phase 2 — commit transaction |
| GET | `/proxy/blocks/latest` | Yes | Recent blocks (pass `?depth=N`) |

### Vehicles (`/api/vehicles`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/vehicles` | Yes | Register vehicle: `{ vin, make, model, year, txid }` |
| GET | `/vehicles/my` | Yes | List authenticated user's vehicles |
| GET | `/vehicles/{vin}` | Yes | Get vehicle by VIN |
| GET | `/vehicles/{vin}/history` | **Public** | Service history timeline DTO |

### Services (`/api/services`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/services` | Yes | Create service entry: `{ vin, serviceType, odometerReading, description, cost, imageIds[], serviceDate, txid }` |
| GET | `/services/my` | Yes | All entries by the current user |
| GET | `/services/vehicle/{vin}` | Yes | Entries for a specific vehicle |
| GET | `/services/{id}` | Yes | Single entry detail |

### Images (`/api/images`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/images/upload` | Yes | Multipart upload (`file` field); returns `{ id, sha256Hash, ... }` |
| GET | `/images/{id}` | Yes | Retrieve image bytes with correct `Content-Type` |

---

## Configuration

```properties
server.port=8080
spring.data.mongodb.uri=mongodb://localhost:27017/pnyx_gateway
px-ledger.url=http://localhost:8081
px-gateway.listener.enabled=true
jwt.secret=YOUR_SUPER_SECRET_KEY_MUST_BE_LONG_ENOUGH
jwt.expiration=600000
jwt.refreshExpiration=604800000
```

Set `px-gateway.listener.enabled=false` during integration tests to suppress the SSE subscription.

---

## Running

1. Start MongoDB
2. Start `px-ledger` on port 8081
3. Run:

```bash
mvn spring-boot:run
```

API available at `http://localhost:8080`.

---

## Testing

```bash
mvn clean test
```

Key test classes: `AuthControllerTest`, `LedgerClientServiceTest`, `BlockEventProcessorTest`.
