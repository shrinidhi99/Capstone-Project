# Product Catalog Service

This microservice handles product listing, product details, category browsing, keyword search, stock updates, and soft deletion for the e-commerce platform.

---

## Running MySQL via Docker

This service can use a MySQL database (`products_db`) running locally in Docker. It uses port `3308` on the host machine so it does not conflict with UserService MySQL on `3307`.

### Step 1: Build the MySQL Image

Open a terminal and navigate to the ProductCatalogService directory:

```bash
cd ProductCatalogService
```

Then build the MySQL Docker image:

```bash
docker build -t ecommerce-products-mysql ./docker
```

### Step 2: Run the MySQL Container

```bash
docker volume create product-mysql-data

docker run --name product-mysql -p 3308:3306 -v product-mysql-data:/var/lib/mysql -d ecommerce-products-mysql
```

### Step 3: Configure the Spring Boot App

Set these environment variables in IntelliJ run configuration or in your terminal before starting the app:

```text
PRODUCT_DB_URL=jdbc:mysql://localhost:3308/products_db
PRODUCT_DB_DRIVER=com.mysql.cj.jdbc.Driver
PRODUCT_DB_USERNAME=productadmin
PRODUCT_DB_PASSWORD=productpass
```

Without these variables, the service falls back to the in-memory H2 database for quick local testing.

### Step 4: Verify the Database

Connect from MySQL Workbench or any MySQL client:

```text
Host: localhost
Port: 3308
Database: products_db
Username: productadmin
Password: productpass
```

Run:

```sql
select * from products_db.products;
```

---

## API Documentation

After starting the Spring Boot app, open:

```text
http://localhost:8081/swagger-ui
```

Useful endpoints:

- `POST /products`
- `GET /products`
- `GET /products?category=Mobiles`
- `GET /products?search=iphone`
- `GET /products/categories`
- `GET /products/{productId}`
- `PUT /products/{productId}`
- `PATCH /products/{productId}/stock`
- `DELETE /products/{productId}`

---

## Quick Postman Flow

Base URL:

```text
http://localhost:8081
```

Use this header for create/update requests:

```text
Content-Type: application/json
```

### 1) Health Check

```http
GET /health/app
```

### 2) Create Product

```http
POST /products
```

Request body:

```json
{
  "name": "MacBook Air M3",
  "description": "Apple laptop with M3 chip, 13-inch display, and all-day battery life.",
  "category": "Laptops",
  "brand": "Apple",
  "price": 114900.00,
  "quantity": 10,
  "imageUrl": "https://example.com/images/macbook-air-m3.jpg"
}
```

### 3) Browse Products

```http
GET /products
GET /products?category=Mobiles
GET /products?search=iphone
GET /products/categories
GET /products/1
```

### 4) Update Product

```http
PUT /products/1
```

Request body:

```json
{
  "name": "MacBook Air M3",
  "description": "Apple laptop with M3 chip, 13-inch display, and all-day battery life.",
  "category": "Laptops",
  "brand": "Apple",
  "price": 109900.00,
  "quantity": 8,
  "imageUrl": "https://example.com/images/macbook-air-m3.jpg"
}
```

### 5) Update Stock

```http
PATCH /products/1/stock
```

Request body:

```json
{
  "quantity": 15
}
```

### 6) Soft Delete Product

```http
DELETE /products/1
```

Soft-deleted products are hidden from normal browse/search APIs. To include inactive products:

```http
GET /products?includeInactive=true
```

---

## Notes

- The service uses MySQL when the `PRODUCT_DB_*` environment variables are configured.
- The service falls back to H2 when those variables are not configured, which is useful for quick local testing.
- Search is implemented with Spring Data JPA queries for the MVP. Elasticsearch can be added later as a scalability enhancement.
