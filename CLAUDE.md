# px-gateway — Claude Code Guide

## Project Overview

Spring Boot 3.x API gateway for the Pnyx ecosystem. Acts as middleware between `px-web-portal` (mobile app) and `px-ledger` (blockchain node). Handles auth, MongoDB persistence, image storage, and SSE→WebSocket event bridging.

Active branch: `feature/vehicle-ledger`. Runs on port **8080**. Requires MongoDB and px-ledger (port 8081).

---

## Commands

```bash
mvn spring-boot:run      # start the gateway
mvn clean test           # run test suite
```

---

## Package Structure

```
com.pnyx.gateway/
  config/          # SecurityConfig (CORS, JWT filter chain), WebSocketConfig
  controller/      # REST controllers (one per domain)
  dto/             # Request/response POJOs and records
  exception/       # GlobalExceptionHandler, AuthEntryPoint, AccessDeniedHandler
  filter/          # JwtAuthFilter, RequestLoggingFilter
  listener/        # BlockEventProcessor (SSE → status updates)
  model/           # MongoDB documents: User, Vehicle, ServiceEntry, ImageDocument, RefreshToken
  repository/      # Spring Data MongoDB repositories
  service/         # Business logic + LedgerClientService (WebClient proxy)
  util/            # JwtUtil
```

---

## API Endpoints

### Auth (`/api/auth`)
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/register` | No | `{ username, password, role? }` — role defaults to `OWNER` |
| POST | `/login` | No | Returns `{ accessToken, refreshToken, role, walletId }` |
| POST | `/refresh` | No | Exchange refresh token |

### Wallet Proxy (`/api/proxy`)
Proxies to px-ledger. Passes through `type` and `metadata` fields on lock requests.
| Method | Path | Auth |
|--------|------|------|
| POST | `/proxy/wallets` | Yes — creates ledger wallet, saves `walletId` on User |
| GET | `/proxy/{walletId}/nonce` | Yes |
| POST | `/proxy/lock` | Yes |
| POST | `/proxy/commit` | Yes |
| GET | `/proxy/blocks/latest` | Yes |

### Vehicles (`/api/vehicles`)
| Method | Path | Auth |
|--------|------|------|
| POST | `/vehicles` | Yes — `{ vin, make, model, year, txid }` |
| GET | `/vehicles/my` | Yes |
| GET | `/vehicles/{vin}` | Yes |
| GET | `/vehicles/{vin}/history` | **Public** — returns shaped DTO with `serviceHistory[]` |

### Services (`/api/services`)
| Method | Path | Auth |
|--------|------|------|
| POST | `/services` | Yes — `{ vin, serviceType, odometerReading, description, cost, imageIds[], serviceDate, txid }` |
| GET | `/services/my` | Yes |
| GET | `/services/vehicle/{vin}` | Yes |
| GET | `/services/{id}` | Yes |

### Images (`/api/images`)
| Method | Path | Auth |
|--------|------|------|
| POST | `/images/upload` | Yes — multipart `file` field |
| GET | `/images/{id}` | Yes — returns bytes with correct Content-Type |

---

## Key Design Decisions

**JWT claims**: Token embeds `role` (`OWNER` or `SHOP`). `PnyxUserDetailsService` returns `ROLE_OWNER` / `ROLE_SHOP` authority.

**Lock request passthrough**: `LockRequest` DTO has `type` and `metadata` fields forwarded to px-ledger. Vehicle/service txns use `amount=0` — px-ledger skips balance check when amount is zero.

**dataHash** (ServiceEntry): SHA-256 of canonical string `vin|serviceType|odometer|walletId|sortedImageHashes` — computed server-side after image upload.

**Image storage**: `MongoImageStorageService` stores bytes + SHA-256 hash in MongoDB. `associatedEntityId` field reserved for future linking; swap to IPFS by implementing `ImageStorageService`.

**BlockEventProcessor**: SSE subscription to px-ledger. On `BLOCK_MINED`, fetches block transactions. For `SERVICE_ENTRY` type txns, updates matching `ServiceEntry.status` `PENDING → CONFIRMED` by `blockchainTxid`.

---

## MongoDB Collections

| Collection | Model | Key Indexes |
|-----------|-------|-------------|
| `users` | `User` | `username` (unique), `walletId` |
| `refresh_tokens` | `RefreshToken` | `token`, `username` |
| `vehicles` | `Vehicle` | `vin` (unique), `ownerUsername` |
| `service_entries` | `ServiceEntry` | `vin`, `submitterUsername`, `blockchainTxid` |
| `image_documents` | `ImageDocument` | `associatedEntityId` |

---

## Configuration (`application.properties`)

```properties
server.port=8080
spring.data.mongodb.uri=mongodb://localhost:27017/pnyx_gateway
px-ledger.url=http://localhost:8081
px-gateway.listener.enabled=true
jwt.secret=YOUR_SECRET_KEY
jwt.expiration=600000
jwt.refreshExpiration=604800000
```

Set `px-gateway.listener.enabled=false` in integration tests to suppress the SSE subscription.
