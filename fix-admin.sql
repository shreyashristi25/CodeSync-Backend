INSERT INTO auth_db.users (email, full_name, password_hash, provider, role, status, created_at) 
VALUES ('testadmin@codesync.com', 'Test Admin User', '$2a$10$QfDwtmilf/CoubG/8Ne5I.eqxtAKds/S7odouVqd.9YOKa5T85k62', 'LOCAL', 'ADMIN', 'ACTIVE', NOW()) 
ON DUPLICATE KEY UPDATE password_hash = '$2a$10$QfDwtmilf/CoubG/8Ne5I.eqxtAKds/S7odouVqd.9YOKa5T85k62';