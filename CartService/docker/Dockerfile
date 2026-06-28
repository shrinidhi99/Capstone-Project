FROM mongo:7

ENV MONGO_INITDB_ROOT_USERNAME=mongoadmin
ENV MONGO_INITDB_ROOT_PASSWORD=mongopass
ENV MONGO_INITDB_DATABASE=cart_db

COPY init.js /docker-entrypoint-initdb.d/
