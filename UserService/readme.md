# 🧾 User Management Service

This microservice handles user registration, authentication, profile management, and password reset functionality for the e-commerce platform.

---

## 🐳 Running MySQL via Docker

This service expects a MySQL database (`users_db`) to be running locally. Follow these steps:
### 🛠️ Step 1: Build the MySQL Image
Open the terminal and navigate to the root directory of the project. Then, run the following command to build the MySQL Docker image:
```bash
docker build -t ecommerce-backend-mysql ./docker
```
### 🛠️ Step 2: Run the MySQL Container
```bash
docker volume create user-mysql-data

docker run --name user-mysql -p 3307:3306 -v user-mysql-data:/var/lib/mysql -d ecommerce-backend-mysql
```
Verify the MySQL container running status by connecting to it via MySQL Workbench or any other MySQL client using the credentials provided in the Dockerfile.
Run the following SQL query in the MySQL query console to check the created database and table:
```sql
select * from users_db.users;
```

---

## 🔐 Using the APIs

This service uses JWT bearer tokens for protected endpoints.

### 1) Log in to get a fresh token

`POST /user/login`

Request body:
```json
{
	"email": "user@example.com",
	"password": "your-password"
}
```

Successful response:
```json
{
	"token": "<jwt-token>"
}
```

### 2) Send the token on protected requests

In Postman, set the Authorization header to:

`Authorization: Bearer <jwt-token>`

The token expires after 15 minutes, so if you reopen Postman later you will usually need to log in again and copy a new token.

### 3) Endpoints

Public endpoints:
- `POST /user/register`
- `POST /user/login`
- `POST /user/forgot-password`
- `POST /user/reset-password`
- `GET /health/app`
- `GET /health/db`
- Swagger UI at `/swagger-ui`

Protected endpoints:
- `GET /user/details?email=...`
- `PUT /user/update-profile`
- `PUT /user/update-role`
- `DELETE /user?email=...`

### 4) Quick Postman flow

1. Call `POST /user/login` with a valid email and password.
2. Copy the returned token.
3. Paste it into the Bearer Token field for the next request.
4. Re-login whenever you see `403` from an expired or missing token.

---

## Password Reset

Password reset uses Gmail SMTP to deliver a one-time token to the user's email.

### Environment variable required

Set this in IntelliJ Run Configuration → Environment Variables before starting the service:

```
GMAIL_APP_PASSWORD=<16-character-app-password>
```

Generate an App Password at: Google Account → Security → 2-Step Verification → App Passwords.

### Flow

**Step 1** — Request a reset token:

`POST /user/forgot-password`

```json
{
  "email": "user@example.com"
}
```

The token is emailed to the address. It expires in 15 minutes.

**Step 2** — Reset the password:

`POST /user/reset-password`

```json
{
  "token": "<token-from-email>",
  "newPassword": "newpassword123"
}
```

---

## Kafka Events

Registration publishes a `user.registered` event to Kafka.

**Topic:** `user.registered`

**Payload:**
```json
{
  "userId": 1,
  "name": "John Doe",
  "email": "user@example.com",
  "role": "USER",
  "registeredAt": "2026-06-28T23:22:30"
}
```

Kafka must be running on `localhost:9092` before starting the service. Override with env var `USER_KAFKA_SERVERS`.