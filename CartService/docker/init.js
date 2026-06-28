db = db.getSiblingDB('cart_db');

db.createUser({
    user: 'cartadmin',
    pwd: 'cartpass',
    roles: [{ role: 'readWrite', db: 'cart_db' }]
});

db.createCollection('carts');
