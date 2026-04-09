-- Create Admin User for BookMyJuice
-- Date: January 9, 2026
-- Username: support
-- Email: support@bookmyjuice.co.in
-- Password: rADHASOAMI@0 (BCrypt hash below)

USE bmj_db;

-- Delete existing user if exists
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE email='support@bookmyjuice.co.in');
DELETE FROM users WHERE email='support@bookmyjuice.co.in';

-- Create admin user
INSERT INTO users (username, email, password, first_name, last_name, address, city, state, country, zip)
VALUES (
  'support',
  'support@bookmyjuice.co.in',
  '$2a$10$gQmfYPvXE.dqYe49yJ7imuCWkWCXKg7xI.bVBb4VPhTwHNqJPqFaG',
  'Support',
  'Team',
  'Main Street',
  'Bangalore',
  'Karnataka',
  'IN',
  '560001'
);

-- Add ROLE_ADMIN to the user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.email='support@bookmyjuice.co.in' 
  AND r.name='ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- Verify
SELECT 'Admin user created successfully!' as status;
SELECT COUNT(*) as admin_user_count FROM users WHERE email='support@bookmyjuice.co.in';
SELECT u.id, u.username, u.email, r.name as role
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.email='support@bookmyjuice.co.in';
