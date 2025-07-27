FROM mysql:8.0

# Environment variables for MySQL setup
ENV MYSQL_ROOT_PASSWORD=rootpass
ENV MYSQL_DATABASE=users_db
ENV MYSQL_USER=useradmin
ENV MYSQL_PASSWORD=userpass

# Copy initial schema file
COPY init.sql /docker-entrypoint-initdb.d/
