-- Create super admin user for CodeSync
-- This script creates the super admin with known credentials
-- Email: admin@codesync.local
-- Password: admin123

INSERT INTO auth_db.users (email, full_name, password_hash, provider, role, status, created_at) 
VALUES (
    'admin@codesync.local', 
    'Super Admin', 
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7pFdldqp8N6ds2yZqCm9R2IG', 
    'LOCAL', 
    'ADMIN', 
    'ACTIVE', 
    NOW()
) ON DUPLICATE KEY UPDATE 
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7pFdldqp8N6ds2yZqCm9R2IG',
    role = 'ADMIN',
    status = 'ACTIVE';

-- Verify the user was created/updated
SELECT email, role, status FROM users WHERE email = 'admin@codesync.local';