-- ============================================================
-- Flyway Migration V6: Seed Test Accounts
-- ============================================================
-- Creates predefined test accounts for E2E testing.
-- Passwords are BCrypt hashed (all passwords = "Test@1234")
-- BCrypt hash: $2b$12$mpZ8lsANcb/B4X5gQsxoM.vUsJoIx5gnx9EKQa5vDb/5OzHzweM22 (Test@1234)
-- ============================================================

-- Insert roles if not already present
INSERT IGNORE INTO roles (id, name) VALUES (1, 'ROLE_USER');
INSERT IGNORE INTO roles (id, name) VALUES (2, 'ROLE_MODERATOR');
INSERT IGNORE INTO roles (id, name) VALUES (3, 'ROLE_ADMIN');

-- Test Account TA-01: Primary test user (username = phone number)
-- ID is manually set (below sequence start of 500) for stability
INSERT IGNORE INTO users (id, username, email, password, first_name, last_name, phone, token_version)
VALUES (1, '9999999901', 'ta01@bookmyjuice.co.in',
        '$2b$12$mpZ8lsANcb/B4X5gQsxoM.vUsJoIx5gnx9EKQa5vDb/5OzHzweM22',
        'Test', 'User One', '9999999901', 1);

-- Test Account TA-02: Secondary test user
INSERT IGNORE INTO users (id, username, email, password, first_name, last_name, phone, token_version)
VALUES (2, '9999999902', 'ta02@bookmyjuice.co.in',
        '$2b$12$mpZ8lsANcb/B4X5gQsxoM.vUsJoIx5gnx9EKQa5vDb/5OzHzweM22',
        'Test', 'User Two', '9999999902', 1);

-- Test Account TA-03: Admin test user
INSERT IGNORE INTO users (id, username, email, password, first_name, last_name, phone, token_version)
VALUES (3, '9999999903', 'ta03@bookmyjuice.co.in',
        '$2b$12$mpZ8lsANcb/B4X5gQsxoM.vUsJoIx5gnx9EKQa5vDb/5OzHzweM22',
        'Admin', 'User', '9999999903', 1);

-- Assign roles to test accounts
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (1, 1); -- TA-01: ROLE_USER
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (2, 1); -- TA-02: ROLE_USER
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (3, 1); -- TA-03: ROLE_USER
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (3, 3); -- TA-03: ROLE_ADMIN
