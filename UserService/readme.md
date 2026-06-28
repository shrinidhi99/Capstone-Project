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