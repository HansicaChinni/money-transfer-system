-- =============================================================================
-- FlowPay Money Transfer System - Full Schema + Seed Data
-- =============================================================================
-- Usage:
--   mysql -u root -p < seed-data.sql
--   Default password for all test users is "pass123"
-- =============================================================================

CREATE DATABASE IF NOT EXISTS bankdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bankdb;

DROP TABLE IF EXISTS redemption_requests;
DROP TABLE IF EXISTS reward_transactions;
DROP TABLE IF EXISTS reward_items;
DROP TABLE IF EXISTS account_rewards;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS transaction_logs;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS accounts;

-- =============================================================================
-- TABLES
-- =============================================================================

CREATE TABLE accounts (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(255),
    holder_name   VARCHAR(100)  NOT NULL,
    balance       DECIMAL(19,2) NOT NULL,
    status        ENUM('ACTIVE','CLOSED','LOCKED') NOT NULL,
    version       BIGINT,
    last_updated  DATETIME(6)   NOT NULL,
    daily_transfer_limit DECIMAL(19,2),
    daily_transferred     DECIMAL(19,2),
    daily_reset_date      DATE,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       ENUM('ADMIN','USER') NOT NULL,
    account_id BIGINT,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    CONSTRAINT fk_user_account FOREIGN KEY (account_id) REFERENCES accounts(id)
) ENGINE=InnoDB;

CREATE TABLE transaction_logs (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    from_account_id  BIGINT        NOT NULL,
    to_account_id    BIGINT        NOT NULL,
    amount           DECIMAL(19,2) NOT NULL,
    status           ENUM('FAILED','SUCCESS') NOT NULL,
    failure_reason   VARCHAR(255),
    idempotency_key  VARCHAR(64)   NOT NULL,
    created_on       DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tx_idempotency (idempotency_key),
    INDEX idx_tx_from_account (from_account_id),
    INDEX idx_tx_to_account (to_account_id),
    INDEX idx_tx_created_on (created_on)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    action        VARCHAR(50)  NOT NULL,
    entity_type   VARCHAR(50)  NOT NULL,
    entity_id     BIGINT       NOT NULL,
    performed_by  VARCHAR(100) NOT NULL,
    old_value     TEXT,
    new_value     TEXT,
    timestamp     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE account_rewards (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    account_id    BIGINT      NOT NULL,
    total_points  INT         NOT NULL,
    version       BIGINT,
    last_updated  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rewards_account (account_id),
    CONSTRAINT fk_rewards_account FOREIGN KEY (account_id) REFERENCES accounts(id)
) ENGINE=InnoDB;

CREATE TABLE reward_items (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    name             VARCHAR(100)  NOT NULL,
    description      VARCHAR(500),
    brand            VARCHAR(100)  NOT NULL,
    points_required  INT           NOT NULL,
    coupon_value     DECIMAL(10,2),
    image_url        VARCHAR(255),
    is_active        BIT           NOT NULL,
    created_at       DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE reward_transactions (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    account_id     BIGINT        NOT NULL,
    transaction_id BIGINT        NOT NULL,
    points_earned  INT           NOT NULL,
    amount         DECIMAL(19,2) NOT NULL,
    reason         VARCHAR(255),
    created_on     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_reward_account (account_id),
    INDEX idx_reward_tx (transaction_id),
    CONSTRAINT fk_reward_tx_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_reward_tx_log FOREIGN KEY (transaction_id) REFERENCES transaction_logs(id)
) ENGINE=InnoDB;

CREATE TABLE redemption_requests (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    account_id     BIGINT      NOT NULL,
    reward_item_id BIGINT      NOT NULL,
    points_spent   INT         NOT NULL,
    status         ENUM('CANCELLED','FULFILLED','PENDING') NOT NULL,
    coupon_code    VARCHAR(30),
    notes          VARCHAR(255),
    created_on     DATETIME(6) NOT NULL,
    fulfilled_on   DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_redemption_account (account_id),
    INDEX idx_redemption_status (status),
    CONSTRAINT fk_redemption_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_redemption_item FOREIGN KEY (reward_item_id) REFERENCES reward_items(id)
) ENGINE=InnoDB;

-- =============================================================================
-- SEED DATA
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. REWARD ITEMS (5 sample coupons)
-- ---------------------------------------------------------------------------
INSERT INTO reward_items (id, name, description, brand, points_required, coupon_value, image_url, is_active, created_at)
VALUES
  (1, 'Amazon Gift Card',    'Discount coupon for Amazon',    'Amazon',  500, 250.00, 'https://placehold.co/300x200/FF9900/white?text=Amazon',   TRUE, '2026-06-01 00:00:00'),
  (2, 'Swiggy Food Voucher', 'Discount coupon for Swiggy',    'Swiggy',  200, 100.00, 'https://placehold.co/300x200/FC8019/white?text=Swiggy',   TRUE, '2026-06-01 00:00:01'),
  (3, 'Myntra Fashion Coupon','Discount coupon for Myntra',   'Myntra',  350, 175.00, 'https://placehold.co/300x200/E91E63/white?text=Myntra',   TRUE, '2026-06-01 00:00:02'),
  (4, 'Uber Ride Discount',  'Discount coupon for Uber',      'Uber',    250, 125.00, 'https://placehold.co/300x200/000000/white?text=Uber',      TRUE, '2026-06-01 00:00:03'),
  (5, 'Zomato Dining Voucher','Discount coupon for Zomato',   'Zomato',  150,  75.00, 'https://placehold.co/300x200/E23744/white?text=Zomato',   TRUE, '2026-06-01 00:00:04');

-- ---------------------------------------------------------------------------
-- 2. ACCOUNTS (15 diverse accounts)
-- ---------------------------------------------------------------------------
-- Balances range from ₹0 to ₹5,00,000
-- Statuses: ACTIVE, LOCKED, CLOSED
INSERT INTO accounts (id, account_number, holder_name, balance, status, version, last_updated, daily_transfer_limit, daily_transferred, daily_reset_date)
VALUES
  (1,  'ACC-2026-000001', 'Rajesh Kumar',       250000.00, 'ACTIVE', 0, '2026-06-17 08:00:00', 100000.00, 5000.00,  '2026-06-17'),
  (2,  'ACC-2026-000002', 'Priya Sharma',        150000.00, 'ACTIVE', 0, '2026-06-17 08:05:00', 100000.00, 25000.00, '2026-06-17'),
  (3,  'ACC-2026-000003', 'Amit Patel',          500000.00, 'ACTIVE', 0, '2026-06-17 08:10:00', 200000.00, 0.00,     '2026-06-17'),
  (4,  'ACC-2026-000004', 'Sneha Reddy',          75000.00, 'ACTIVE', 0, '2026-06-16 14:30:00', 100000.00, 0.00,     '2026-06-17'),
  (5,  'ACC-2026-000005', 'Vikram Singh',         32000.00, 'ACTIVE', 0, '2026-06-17 09:15:00', 100000.00, 8000.00,  '2026-06-17'),
  (6,  'ACC-2026-000006', 'Ananya Gupta',          2500.00, 'ACTIVE', 0, '2026-06-16 10:00:00', 50000.00,  0.00,     '2026-06-17'),
  (7,  'ACC-2026-000007', 'Rohit Joshi',            500.00, 'ACTIVE', 0, '2026-06-15 11:20:00', 25000.00,  0.00,     '2026-06-17'),
  (8,  'ACC-2026-000008', 'Neha Verma',             100.00, 'ACTIVE', 0, '2026-06-14 16:45:00', 10000.00,  0.00,     '2026-06-17'),
  (9,  'ACC-2026-000009', 'Arjun Nair',              0.00, 'ACTIVE', 0, '2026-06-10 12:00:00', 50000.00,  0.00,     '2026-06-17'),
  (10, 'ACC-2026-000010', 'Meera Iyer',           12000.00, 'LOCKED', 0, '2026-06-13 09:30:00', 50000.00,  0.00,     '2026-06-17'),
  (11, 'ACC-2026-000011', 'Karan Mehta',           45000.00, 'LOCKED', 0, '2026-06-12 15:00:00', 100000.00, 0.00,     '2026-06-17'),
  (12, 'ACC-2026-000012', 'Divya Kapoor',            0.00, 'CLOSED', 0, '2026-06-01 10:00:00', NULL,       NULL,     NULL),
  (13, 'ACC-2026-000013', 'Siddharth Rao',           0.00, 'CLOSED', 0, '2026-05-20 11:00:00', NULL,       NULL,     NULL),
  (14, 'ACC-2026-000014', 'System Reserve Fund', 1000000.00, 'ACTIVE', 0, '2026-06-17 00:00:00', 500000.00, 0.00,     '2026-06-17'),
  (15, 'ACC-2026-000015', 'Admin Operational',    200000.00, 'ACTIVE', 0, '2026-06-17 00:00:00', 200000.00, 0.00,     '2026-06-17');

-- ---------------------------------------------------------------------------
-- 3. USERS (1 admin + 14 users, password = "pass123" BCrypt-encrypted)
-- ---------------------------------------------------------------------------
INSERT INTO users (id, username, password, role, account_id, created_at)
VALUES
  (1,  'admin',      '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'ADMIN', NULL, '2026-01-01 00:00:00'),
  (2,  'rajesh',     '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  1,    '2026-01-01 00:01:00'),
  (3,  'priya',      '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  2,    '2026-01-01 00:02:00'),
  (4,  'amit',       '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  3,    '2026-01-02 00:00:00'),
  (5,  'sneha',      '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  4,    '2026-01-03 00:00:00'),
  (6,  'vikram',     '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  5,    '2026-01-04 00:00:00'),
  (7,  'ananya',     '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  6,    '2026-01-05 00:00:00'),
  (8,  'rohit',      '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  7,    '2026-01-06 00:00:00'),
  (9,  'neha',       '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  8,    '2026-01-07 00:00:00'),
  (10, 'arjun',      '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  9,    '2026-01-08 00:00:00'),
  (11, 'meera',      '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  10,   '2026-01-09 00:00:00'),
  (12, 'karan',      '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  11,   '2026-01-10 00:00:00'),
  (13, 'divya',      '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  12,   '2026-01-11 00:00:00'),
  (14, 'siddharth',  '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'USER',  13,   '2026-01-12 00:00:00'),
  (15, 'system',     '$2a$10$XYnBuNrvL6IqGzO3tRFExuLnIA8x46OvCO6UpMzsy7izh5aKJT3TG', 'ADMIN', 14,   '2026-01-01 00:00:00');

-- ---------------------------------------------------------------------------
-- 4. TRANSACTION LOGS (30 entries — mix of successes, failures, scenarios)
-- ---------------------------------------------------------------------------
INSERT INTO transaction_logs (id, from_account_id, to_account_id, amount, status, failure_reason, idempotency_key, created_on)
VALUES
  (1,  1,  2,   5000.00, 'SUCCESS', NULL, 'ik-20260601-001', '2026-06-01 09:00:00'),
  (2,  2,  3,  10000.00, 'SUCCESS', NULL, 'ik-20260601-002', '2026-06-01 10:30:00'),
  (3,  3,  4,  15000.00, 'SUCCESS', NULL, 'ik-20260602-001', '2026-06-02 11:00:00'),
  (4,  5,  1,   3000.00, 'SUCCESS', NULL, 'ik-20260602-002', '2026-06-02 14:00:00'),
  (5,  4,  5,   2000.00, 'SUCCESS', NULL, 'ik-20260603-001', '2026-06-03 09:15:00'),
  (6,  6,  1,   5000.00, 'FAILED', 'Insufficient balance', 'ik-20260603-002', '2026-06-03 10:00:00'),
  (7,  1,  1,  10000.00, 'FAILED', 'Self-transfer not allowed', 'ik-20260603-003', '2026-06-03 11:00:00'),
  (8,  10, 1,   5000.00, 'FAILED', 'Account is LOCKED', 'ik-20260603-004', '2026-06-03 12:00:00'),
  (9,  12, 1,   1000.00, 'FAILED', 'Account is CLOSED', 'ik-20260604-001', '2026-06-04 09:00:00'),
  (10, 2,  3,  10000.00, 'FAILED', 'Amount exceeds daily limit', 'ik-20260604-002', '2026-06-04 10:00:00'),
  (11, 1,  3,  20000.00, 'SUCCESS', NULL, 'ik-20260605-001', '2026-06-05 08:00:00'),
  (12, 3,  2,   5000.00, 'SUCCESS', NULL, 'ik-20260605-002', '2026-06-05 09:00:00'),
  (13, 5,  2,   8000.00, 'SUCCESS', NULL, 'ik-20260606-001', '2026-06-06 10:00:00'),
  (14, 4,  6,   5000.00, 'SUCCESS', NULL, 'ik-20260607-001', '2026-06-07 11:00:00'),
  (15, 1,  5,  12000.00, 'SUCCESS', NULL, 'ik-20260608-001', '2026-06-08 12:00:00'),
  (16, 7,  1,   1000.00, 'FAILED', 'Insufficient balance', 'ik-20260608-002', '2026-06-08 13:00:00'),
  (17, 8,  2,    200.00, 'FAILED', 'Insufficient balance', 'ik-20260609-001', '2026-06-09 09:00:00'),
  (18, 11, 4,  10000.00, 'FAILED', 'Account is LOCKED',   'ik-20260609-002', '2026-06-09 10:00:00'),
  (19, 13, 5,   5000.00, 'FAILED', 'Account is CLOSED',   'ik-20260610-001', '2026-06-10 08:00:00'),
  (20, 1,  2,   -500.00, 'FAILED', 'Invalid amount',      'ik-20260610-002', '2026-06-10 09:00:00'),
  (21, 2,  1,  15000.00, 'SUCCESS', NULL, 'ik-20260612-001', '2026-06-12 08:00:00'),
  (22, 3,  5,  25000.00, 'SUCCESS', NULL, 'ik-20260613-001', '2026-06-13 09:30:00'),
  (23, 1,  4,   5000.00, 'SUCCESS', NULL, 'ik-20260614-001', '2026-06-14 10:00:00'),
  (24, 5,  3,   7000.00, 'SUCCESS', NULL, 'ik-20260615-001', '2026-06-15 11:15:00'),
  (25, 4,  1,   3000.00, 'SUCCESS', NULL, 'ik-20260616-001', '2026-06-16 08:45:00'),
  (26, 1,  2,   5000.00, 'SUCCESS', NULL, 'ik-20260617-001', '2026-06-17 08:00:00'),
  (27, 2,  3,  20000.00, 'SUCCESS', NULL, 'ik-20260617-002', '2026-06-17 09:00:00'),
  (28, 3,  1,  10000.00, 'SUCCESS', NULL, 'ik-20260617-003', '2026-06-17 10:00:00'),
  (29, 6,  2,    500.00, 'FAILED', 'Insufficient balance', 'ik-20260617-004', '2026-06-17 11:00:00'),
  (30, 1,  2,   5000.00, 'FAILED', 'Amount exceeds daily limit', 'ik-20260617-005', '2026-06-17 11:30:00');

-- ---------------------------------------------------------------------------
-- 5. AUDIT LOGS (sample entries for traceability)
-- ---------------------------------------------------------------------------
INSERT INTO audit_logs (id, action, entity_type, entity_id, performed_by, old_value, new_value, timestamp)
VALUES
  (1, 'STATUS_CHANGE', 'Account', 10, 'admin', 'ACTIVE', 'LOCKED',   '2026-06-13 09:30:00'),
  (2, 'STATUS_CHANGE', 'Account', 11, 'admin', 'ACTIVE', 'LOCKED',   '2026-06-12 15:00:00'),
  (3, 'STATUS_CHANGE', 'Account', 12, 'admin', 'ACTIVE', 'CLOSED',   '2026-06-01 10:00:00'),
  (4, 'STATUS_CHANGE', 'Account', 13, 'admin', 'ACTIVE', 'CLOSED',   '2026-05-20 11:00:00'),
  (5, 'PASSWORD_CHANGE','User',   3,  'priya', '****',   '****',      '2026-03-15 14:00:00'),
  (6, 'ACCOUNT_CREATED','Account',14, 'admin', NULL,     'ACC-2026-000014', '2026-01-01 00:00:00');

-- ---------------------------------------------------------------------------
-- 6. ACCOUNT REWARDS (users who have earned points via transfers)
-- ---------------------------------------------------------------------------
-- Points: 1 point per 200 transferred (only SUCCESS, amount > 100)
INSERT INTO account_rewards (id, account_id, total_points, version, last_updated)
VALUES
  (1, 1, 300, 0, '2026-06-17 08:00:00'),
  (2, 2, 150, 0, '2026-06-17 09:00:00'),
  (3, 3, 275, 0, '2026-06-17 10:00:00'),
  (4, 4,  50, 0, '2026-06-16 08:45:00'),
  (5, 5,  90, 0, '2026-06-17 09:30:00');

-- ---------------------------------------------------------------------------
-- 7. REWARD TRANSACTIONS (mapping transfers to earned points)
-- ---------------------------------------------------------------------------
INSERT INTO reward_transactions (id, account_id, transaction_id, points_earned, amount, reason, created_on)
VALUES
  (1,  1, 1,    25,  5000.00,  'Transferred ₹5000.00 → 25 points',  '2026-06-01 09:00:00'),
  (2,  2, 2,    50, 10000.00,  'Transferred ₹10000.00 → 50 points', '2026-06-01 10:30:00'),
  (3,  3, 3,    75, 15000.00,  'Transferred ₹15000.00 → 75 points', '2026-06-02 11:00:00'),
  (4,  5, 4,    15,  3000.00,  'Transferred ₹3000.00 → 15 points',  '2026-06-02 14:00:00'),
  (5,  1, 11,  100, 20000.00,  'Transferred ₹20000.00 → 100 points','2026-06-05 08:00:00'),
  (6,  3, 12,   25,  5000.00,  'Transferred ₹5000.00 → 25 points',  '2026-06-05 09:00:00'),
  (7,  5, 13,   40,  8000.00,  'Transferred ₹8000.00 → 40 points',  '2026-06-06 10:00:00'),
  (8,  1, 15,   60, 12000.00,  'Transferred ₹12000.00 → 60 points', '2026-06-08 12:00:00'),
  (9,  2, 21,   75, 15000.00,  'Transferred ₹15000.00 → 75 points', '2026-06-12 08:00:00'),
  (10, 3, 22,  125, 25000.00,  'Transferred ₹25000.00 → 125 points','2026-06-13 09:30:00'),
  (11, 1, 23,   25,  5000.00,  'Transferred ₹5000.00 → 25 points',  '2026-06-14 10:00:00'),
  (12, 5, 24,   35,  7000.00,  'Transferred ₹7000.00 → 35 points',  '2026-06-15 11:15:00'),
  (13, 1, 26,   25,  5000.00,  'Transferred ₹5000.00 → 25 points',  '2026-06-17 08:00:00'),
  (14, 2, 27,  100, 20000.00,  'Transferred ₹20000.00 → 100 points','2026-06-17 09:00:00'),
  (15, 3, 28,   50, 10000.00,  'Transferred ₹10000.00 → 50 points', '2026-06-17 10:00:00');

-- ---------------------------------------------------------------------------
-- 8. REDEMPTION REQUESTS (mix of states)
-- ---------------------------------------------------------------------------
INSERT INTO redemption_requests (id, account_id, reward_item_id, points_spent, status, coupon_code, notes, created_on, fulfilled_on)
VALUES
  (1, 1, 2, 200, 'FULFILLED', 'RWD-A1B2C3D4E5F6G7H8I9J0K', 'Customer requested Swiggy voucher', '2026-06-10 10:00:00', '2026-06-10 14:00:00'),
  (2, 3, 1, 500, 'FULFILLED', 'RWD-Z9Y8X7W6V5U4T3S2R1Q0P9', 'Premium customer - Amazon voucher', '2026-06-12 11:00:00', '2026-06-12 16:30:00'),
  (3, 1, 5, 150, 'PENDING',    NULL, NULL, '2026-06-16 09:00:00', NULL),
  (4, 5, 3, 350, 'CANCELLED', NULL, 'Customer changed mind', '2026-06-14 12:00:00', NULL),
  (5, 2, 4, 250, 'PENDING',    NULL, NULL, '2026-06-17 07:00:00', NULL);

-- =============================================================================
-- VERIFICATION QUERIES (run these to confirm data integrity)
-- =============================================================================
-- SELECT 'Users:        ', COUNT(*) FROM users;
-- SELECT 'Accounts:     ', COUNT(*) FROM accounts;
-- SELECT 'Transactions: ', COUNT(*) FROM transaction_logs;
-- SELECT 'Reward Items: ', COUNT(*) FROM reward_items;
-- SELECT 'Reward Txs:   ', COUNT(*) FROM reward_transactions;
-- SELECT 'Redemptions:  ', COUNT(*) FROM redemption_requests;
-- SELECT 'Audit Logs:   ', COUNT(*) FROM audit_logs;
-- SELECT 'Total balance: ', SUM(balance) FROM accounts WHERE status = 'ACTIVE';
