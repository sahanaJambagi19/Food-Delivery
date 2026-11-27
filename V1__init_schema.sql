-- ============================================================
-- Food Delivery App - V1 Schema Migration
-- ============================================================

-- ROLES
CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- USERS
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    phone        VARCHAR(20)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    role_id      BIGINT NOT NULL,
    is_active    BOOLEAN DEFAULT TRUE,
    profile_image VARCHAR(500),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- REFRESH TOKENS
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    token      VARCHAR(500) NOT NULL UNIQUE,
    user_id    BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked    BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ADDRESSES
CREATE TABLE IF NOT EXISTS addresses (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    label        VARCHAR(50) DEFAULT 'Home',
    address_line VARCHAR(300) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    state        VARCHAR(100) NOT NULL,
    pincode      VARCHAR(10)  NOT NULL,
    latitude     DECIMAL(10,8),
    longitude    DECIMAL(11,8),
    is_default   BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_addr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- RESTAURANTS
CREATE TABLE IF NOT EXISTS restaurants (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id         BIGINT NOT NULL UNIQUE,
    name             VARCHAR(200) NOT NULL,
    description      TEXT,
    cuisine_type     VARCHAR(100),
    address          VARCHAR(300) NOT NULL,
    city             VARCHAR(100) NOT NULL,
    pincode          VARCHAR(10),
    phone            VARCHAR(20),
    email            VARCHAR(150),
    logo_url         VARCHAR(500),
    cover_image_url  VARCHAR(500),
    opening_time     TIME,
    closing_time     TIME,
    avg_delivery_time INT DEFAULT 30,
    min_order_amount DECIMAL(10,2) DEFAULT 0,
    delivery_charge  DECIMAL(10,2) DEFAULT 0,
    avg_rating       DECIMAL(3,2)  DEFAULT 0.00,
    total_ratings    INT DEFAULT 0,
    is_open          BOOLEAN DEFAULT TRUE,
    is_approved      BOOLEAN DEFAULT FALSE,
    is_active        BOOLEAN DEFAULT TRUE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rest_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- FOOD CATEGORIES
CREATE TABLE IF NOT EXISTS food_categories (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(300),
    display_order INT DEFAULT 0,
    is_active     BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_fc_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
);

-- FOOD ITEMS
CREATE TABLE IF NOT EXISTS food_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    category_id   BIGINT NOT NULL,
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    price         DECIMAL(10,2) NOT NULL,
    image_url     VARCHAR(500),
    is_veg        BOOLEAN DEFAULT TRUE,
    is_available  BOOLEAN DEFAULT TRUE,
    avg_rating    DECIMAL(3,2) DEFAULT 0.00,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fi_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    CONSTRAINT fk_fi_category   FOREIGN KEY (category_id)   REFERENCES food_categories(id)
);

-- CARTS
CREATE TABLE IF NOT EXISTS carts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL UNIQUE,
    restaurant_id BIGINT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_rest FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

-- CART ITEMS
CREATE TABLE IF NOT EXISTS cart_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id      BIGINT NOT NULL,
    food_item_id BIGINT NOT NULL,
    quantity     INT NOT NULL DEFAULT 1,
    unit_price   DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_ci_cart FOREIGN KEY (cart_id)      REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_ci_food FOREIGN KEY (food_item_id) REFERENCES food_items(id),
    UNIQUE KEY uq_cart_food (cart_id, food_item_id)
);

-- COUPONS
CREATE TABLE IF NOT EXISTS coupons (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    code              VARCHAR(50) NOT NULL UNIQUE,
    description       VARCHAR(300),
    discount_type     ENUM('PERCENTAGE','FLAT') NOT NULL,
    discount_value    DECIMAL(10,2) NOT NULL,
    min_order_amount  DECIMAL(10,2) DEFAULT 0,
    max_discount      DECIMAL(10,2),
    usage_limit       INT DEFAULT 1,
    used_count        INT DEFAULT 0,
    valid_from        TIMESTAMP NOT NULL,
    valid_until       TIMESTAMP NOT NULL,
    is_active         BOOLEAN DEFAULT TRUE,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ORDERS
CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number     VARCHAR(50) NOT NULL UNIQUE,
    user_id          BIGINT NOT NULL,
    restaurant_id    BIGINT NOT NULL,
    address_id       BIGINT NOT NULL,
    coupon_id        BIGINT,
    status           ENUM('PLACED','CONFIRMED','PREPARING','READY_FOR_PICKUP','PICKED_UP','OUT_FOR_DELIVERY','DELIVERED','REJECTED','CANCELLED','PAYMENT_FAILED') DEFAULT 'PLACED',
    payment_method   ENUM('ONLINE','COD') NOT NULL,
    payment_status   ENUM('PENDING','PAID','FAILED','REFUNDED') DEFAULT 'PENDING',
    subtotal         DECIMAL(10,2) NOT NULL,
    delivery_charge  DECIMAL(10,2) DEFAULT 0,
    tax_amount       DECIMAL(10,2) DEFAULT 0,
    discount_amount  DECIMAL(10,2) DEFAULT 0,
    total_amount     DECIMAL(10,2) NOT NULL,
    special_instructions TEXT,
    estimated_delivery_time INT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user    FOREIGN KEY (user_id)       REFERENCES users(id),
    CONSTRAINT fk_order_rest    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_order_addr    FOREIGN KEY (address_id)    REFERENCES addresses(id),
    CONSTRAINT fk_order_coupon  FOREIGN KEY (coupon_id)     REFERENCES coupons(id)
);

-- ORDER ITEMS
CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT NOT NULL,
    food_item_id BIGINT NOT NULL,
    name         VARCHAR(200) NOT NULL,
    quantity     INT NOT NULL,
    unit_price   DECIMAL(10,2) NOT NULL,
    total_price  DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_oi_order FOREIGN KEY (order_id)     REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_oi_food  FOREIGN KEY (food_item_id) REFERENCES food_items(id)
);

-- ORDER STATUS HISTORY
CREATE TABLE IF NOT EXISTS order_status_history (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT NOT NULL,
    status     VARCHAR(50) NOT NULL,
    note       VARCHAR(300),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_osh_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- PAYMENTS
CREATE TABLE IF NOT EXISTS payments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT NOT NULL UNIQUE,
    razorpay_order_id   VARCHAR(200),
    razorpay_payment_id VARCHAR(200),
    razorpay_signature  VARCHAR(500),
    amount              DECIMAL(10,2) NOT NULL,
    currency            VARCHAR(10) DEFAULT 'INR',
    status              ENUM('CREATED','SUCCESS','FAILED','REFUNDED') DEFAULT 'CREATED',
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- DELIVERY PARTNERS
CREATE TABLE IF NOT EXISTS delivery_partners (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,
    vehicle_type    VARCHAR(50),
    vehicle_number  VARCHAR(50),
    is_available    BOOLEAN DEFAULT FALSE,
    total_deliveries INT DEFAULT 0,
    total_earnings  DECIMAL(10,2) DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dp_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- DELIVERY ASSIGNMENTS
CREATE TABLE IF NOT EXISTS delivery_assignments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT NOT NULL UNIQUE,
    delivery_partner_id BIGINT NOT NULL,
    status              ENUM('ASSIGNED','PICKED_UP','OUT_FOR_DELIVERY','DELIVERED','FAILED') DEFAULT 'ASSIGNED',
    assigned_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    picked_up_at        TIMESTAMP,
    delivered_at        TIMESTAMP,
    delivery_fee        DECIMAL(10,2) DEFAULT 0,
    CONSTRAINT fk_da_order FOREIGN KEY (order_id)            REFERENCES orders(id),
    CONSTRAINT fk_da_dp    FOREIGN KEY (delivery_partner_id) REFERENCES delivery_partners(id)
);

-- COUPON USAGE
CREATE TABLE IF NOT EXISTS coupon_usage (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id  BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    order_id   BIGINT NOT NULL,
    used_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cu_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT fk_cu_user   FOREIGN KEY (user_id)   REFERENCES users(id),
    CONSTRAINT fk_cu_order  FOREIGN KEY (order_id)  REFERENCES orders(id),
    UNIQUE KEY uq_coupon_user (coupon_id, user_id)
);

-- REVIEWS
CREATE TABLE IF NOT EXISTS reviews (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    order_id      BIGINT NOT NULL UNIQUE,
    rating        INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment       TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rev_user FOREIGN KEY (user_id)       REFERENCES users(id),
    CONSTRAINT fk_rev_rest FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_rev_order FOREIGN KEY (order_id)     REFERENCES orders(id)
);

-- NOTIFICATIONS
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    title      VARCHAR(200) NOT NULL,
    message    TEXT NOT NULL,
    type       VARCHAR(50),
    is_read    BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- INDEXES
CREATE INDEX idx_orders_user       ON orders(user_id);
CREATE INDEX idx_orders_restaurant ON orders(restaurant_id);
CREATE INDEX idx_orders_status     ON orders(status);
CREATE INDEX idx_food_items_rest   ON food_items(restaurant_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_reviews_restaurant ON reviews(restaurant_id);

-- SEED DATA
INSERT IGNORE INTO roles(name) VALUES ('CUSTOMER'),('RESTAURANT_OWNER'),('DELIVERY_PARTNER'),('ADMIN');

-- Admin user (password: Admin@123)
INSERT IGNORE INTO users(name,email,phone,password,role_id,is_active)
VALUES('Admin','admin@foodapp.com','9000000000',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       (SELECT id FROM roles WHERE name='ADMIN'),TRUE);

-- Sample coupons
INSERT IGNORE INTO coupons(code,description,discount_type,discount_value,min_order_amount,max_discount,usage_limit,valid_from,valid_until,is_active)
VALUES
('WELCOME50','50% off on first order','PERCENTAGE',50,100,100,1,'2024-01-01','2025-12-31',TRUE),
('FLAT30','Flat ₹30 off','FLAT',30,150,30,100,'2024-01-01','2025-12-31',TRUE),
('SAVE20','20% off on orders above ₹200','PERCENTAGE',20,200,80,100,'2024-01-01','2025-12-31',TRUE);
