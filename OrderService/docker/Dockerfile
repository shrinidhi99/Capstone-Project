FROM mysql:8.0

ENV MYSQL_ROOT_PASSWORD=rootpass
ENV MYSQL_DATABASE=orders_db
ENV MYSQL_USER=orderadmin
ENV MYSQL_PASSWORD=orderpass

COPY init.sql /docker-entrypoint-initdb.d/
