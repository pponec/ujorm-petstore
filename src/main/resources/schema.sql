CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE pet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT fk_pet_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE pet_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE CASCADE
);

-- Default test data
INSERT INTO category (name) VALUES ('Dogs'), ('Cats'), ('Birds');
INSERT INTO customer (id, name) VALUES (1, 'John Doe');

-- Default users (test/test) 
-- Handled by Java DataInitializer on startup

INSERT INTO pet (name, status, category_id) VALUES
    ('Rex', 'AVAILABLE', 1),
    ('Buddy', 'PENDING', 1),
    ('Micka', 'AVAILABLE', 2),
    ('Tweety', 'SOLD', 3);
