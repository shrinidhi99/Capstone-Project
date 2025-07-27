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
Verify the MySQL container running status by connecting to it via MyDQL Workbench or any other MySQL client using the credentials provided in the Dockerfile.
Run the following SQL query in the MySQL query console to check the created database and table:
```sql
select * from users_db.users;
```