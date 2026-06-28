FROM mysql:8.0

# Environment variables for MySQL setup
ENV MYSQL_ROOT_PASSWORD=rootpass
ENV MYSQL_DATABASE=products_db
ENV MYSQL_USER=productadmin
ENV MYSQL_PASSWORD=productpass

# Copy initial schema file
COPY init.sql /docker-entrypoint-initdb.d/
