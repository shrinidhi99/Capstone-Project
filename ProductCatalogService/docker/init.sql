CREATE DATABASE IF NOT EXISTS products_db;
USE products_db;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    category VARCHAR(80) NOT NULL,
    brand VARCHAR(80),
    price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL,
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active ON products(active);

INSERT INTO products (name, description, category, brand, price, quantity, image_url)
VALUES
    ('iPhone 15', 'Apple smartphone with A16 Bionic chip and dual-camera system.', 'Mobiles', 'Apple', 79999.00, 25, 'https://example.com/images/iphone-15.jpg'),
    ('Galaxy S24', 'Samsung Android smartphone with AMOLED display and AI features.', 'Mobiles', 'Samsung', 74999.00, 30, 'https://example.com/images/galaxy-s24.jpg'),
    ('Sony WH-1000XM5', 'Wireless noise-cancelling headphones with long battery life.', 'Audio', 'Sony', 29990.00, 15, 'https://example.com/images/sony-wh1000xm5.jpg');
