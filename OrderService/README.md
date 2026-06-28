# Order Service

Manages order lifecycle for the ecommerce platform. Listens on the `order.placed` Kafka topic produced by CartService, persists orders to MySQL, and publishes `order.confirmed` events back to Kafka.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /health/app | Service health check |
| GET | /health/db | Database connectivity check |
| GET | /orders/{orderId} | Fetch order by ID |
| GET | /orders/user/{userId} | Fetch all orders for a user (newest first) |
| PATCH | /orders/{orderId}/status | Update order status manually |

Swagger UI: `http://localhost:8083/swagger-ui`

## Order Status Lifecycle

`PENDING` → `CONFIRMED` → `SHIPPED` → `DELIVERED`

Status transitions are manual via the PATCH endpoint.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ORDER_DB_URL` | `jdbc:mysql://localhost:3309/orders_db` | MySQL JDBC URL |
| `ORDER_DB_USERNAME` | `orderadmin` | MySQL username |
| `ORDER_DB_PASSWORD` | `orderpass` | MySQL password |
| `ORDER_KAFKA_SERVERS` | `localhost:9092` | Kafka bootstrap servers |

## Setup

### 1. Create Docker volume

```bash
docker volume create order-mysql-data
```

### 2. Start MySQL

```bash
docker build -t order-mysql ./docker
```

```bash
docker run -d --name order-mysql -p 3309:3306 -v order-mysql-data:/var/lib/mysql order-mysql
```

### 3. Start Kafka (if not already running from CartService)

```bash
docker run -d --name kafka -p 9092:9092 -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true apache/kafka:latest
```

Kafka is shared with CartService — if it's already running, skip this step.

### 4. Configure environment variables in IntelliJ

All values are in `SpringBootProjects/.env`. Add them via **Run > Edit Configurations > Environment Variables** — the ones this service actually uses are `ORDER_DB_URL`, `ORDER_DB_USERNAME`, `ORDER_DB_PASSWORD`, and `ORDER_KAFKA_SERVERS`.

### 5. Run the application

```bash
./gradlew bootRun
```

## Kafka Topics

| Topic | Role | Producer | Consumer |
|-------|------|----------|----------|
| `order.placed` | Consumed to create orders | CartService | OrderService |
| `order.confirmed` | Published after order is saved | OrderService | (future: NotificationService) |

## End-to-End Demo Flow

1. Add items to cart via CartService (`POST /cart/{userId}/items`)
2. Checkout via CartService (`POST /cart/{userId}/checkout`) — fires `order.placed` event
3. OrderService consumes the event and creates the order in MySQL
4. Fetch the created order (`GET /orders/user/{userId}`)
5. Update order status (`PATCH /orders/{orderId}/status` with body `{"status": "CONFIRMED"}`)
