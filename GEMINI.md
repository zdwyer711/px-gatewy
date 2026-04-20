# Pnyx Gateway (`px-gateway`) Context

## Project Overview
`px-gateway` is a secure Spring Boot API Gateway and Authentication server for the Pnyx cryptocurrency ecosystem. It serves as the middleware between the frontend client (React) and the blockchain ledger node (`px-ledger`). It manages user identity, secure sessions, and real-time blockchain event bridging.

## Tech Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.4.0
- **Build Tool:** Maven
- **Database:** MongoDB (User data & Refresh Tokens)
- **Security:** Spring Security, JJWT 0.12.3 (Stateless JWT + Refresh Token rotation)
- **Reactive:** Spring WebFlux (`WebClient`) for SSE consumption
- **Real-time:** Spring WebSocket (STOMP) for frontend updates

## Architecture
The gateway implements a standard layered architecture:
- **Controllers:** REST endpoints for Auth, Wallet, and Transactions.
- **Services:** Business logic for User management and Ledger interaction.
- **Repositories:** MongoDB data access.
- **Listeners:** Reactive event processors for blockchain events.

### Key Flows
1.  **Authentication:** Users register/login to receive JWTs. Refresh tokens are stored in MongoDB and rotated.
2.  **Proxying:** Requests for wallet/transaction data are proxied to the `px-ledger` node via `LedgerClientService`.
3.  **Event Bridging:**
    -   `BlockEventProcessor` connects to `px-ledger` SSE stream (`/v1/api/events/subscribe`).
    -   On `BLOCK_MINED`, it checks for relevant transactions.
    -   Updates are pushed to clients via WebSockets (`/topic/wallets/{walletId}`).

## Build & Run

### Prerequisites
- Java 21+
- MongoDB running on `localhost:27017`
- `px-ledger` running on `http://localhost:8081`

### Commands
- **Build:** `mvn clean install`
- **Run:** `mvn spring-boot:run`
- **Test:** `mvn test`

## Key Directories
- `src/main/java/com/pnyx/gateway/config`: Security & WebSocket configuration.
- `src/main/java/com/pnyx/gateway/controller`: REST API endpoints.
- `src/main/java/com/pnyx/gateway/service`: Core business logic (`LedgerClientService`, `RefreshTokenService`).
- `src/main/java/com/pnyx/gateway/listener`: `BlockEventProcessor` for SSE-to-WebSocket bridging.
- `src/main/java/com/pnyx/gateway/dto`: Data Transfer Objects for API requests/responses.

## Configuration
Managed in `src/main/resources/application.properties`.
Key properties:
- `server.port`: 8080
- `spring.data.mongodb.uri`: MongoDB connection string
- `px-ledger.url`: URL of the ledger node
- `jwt.secret`, `jwt.expiration`, `jwt.refreshExpiration`: Security settings
