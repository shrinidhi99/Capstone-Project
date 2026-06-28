# CartService

Manages the shopping cart — add/remove items, update quantities, view cart with totals, and checkout.
Checkout produces an `order.placed` Kafka event consumed by OrderService.

- **Port:** 8082
- **Primary store:** MongoDB
- **Cache:** Redis (cart lookups served from cache; invalidated on every write)
- **Messaging:** Kafka (produces `order.placed` on checkout)
- **Swagger UI:** http://localhost:8082/swagger-ui

## Prerequisites

### 1. MongoDB (Docker)

Build the custom image and run the container from the `CartService` root:

```bash
docker build -t cart-mongo ./docker
docker volume create cart-mongo-data
docker run -d --name cart-mongo -p 27017:27017 -v cart-mongo-data:/data/db cart-mongo
```

The init script creates the `cart_db` database, `cartadmin` user, and `carts` collection automatically on first start.

### 2. Redis (Docker)

```bash
docker volume create cart-redis-data
docker run -d --name cart-redis -p 6379:6379 -v cart-redis-data:/data redis:7-alpine
```

### 3. Kafka (Docker)

Shared across all services — start once at the project level:

```bash
docker run -d --name capstone-kafka -p 9092:9092 -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@127.0.0.1:9093 -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 apache/kafka:latest
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `CART_MONGO_URI` | `mongodb://localhost:27017/cart_db` | MongoDB connection URI |
| `CART_REDIS_HOST` | `localhost` | Redis host |
| `CART_REDIS_PORT` | `6379` | Redis port |
| `CART_KAFKA_SERVERS` | `localhost:9092` | Kafka bootstrap servers |

**The default MongoDB URI has no credentials.** Since the Docker container requires authentication, set this before running:

```
CART_MONGO_URI=mongodb://cartadmin:cartpass@localhost:27017/cart_db?authSource=cart_db
```

In IntelliJ: Run → Edit Configurations → Environment Variables.

## Running the Service

```bash
./gradlew bootRun
```

## API Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/cart/{userId}/items` | Add item to cart |
| GET | `/cart/{userId}` | View cart with totals |
| PATCH | `/cart/{userId}/items/{productId}` | Update item quantity |
| DELETE | `/cart/{userId}/items/{productId}` | Remove item from cart |
| DELETE | `/cart/{userId}` | Clear entire cart |
| POST | `/cart/{userId}/checkout` | Checkout — fires Kafka event, clears cart |
| GET | `/health/app` | Service health check |
| GET | `/health/db` | MongoDB health check |
