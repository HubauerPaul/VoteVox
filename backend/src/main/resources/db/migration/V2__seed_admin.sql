-- =====================================================================
-- VoteVox - V2: Default Admin
-- =====================================================================
-- Inserts a placeholder admin row. The placeholder hash is detected by
-- the application's DataInitializer on startup and replaced with a fresh
-- BCrypt hash for password "Admin1234!". This avoids hard-coding a hash
-- that may not match the BCrypt configuration of the running JVM.
-- =====================================================================

INSERT INTO admin_users (id, name, email, password_hash, role)
VALUES (
    gen_random_uuid(),
    'Default Admin',
    'admin@votevox.at',
    'BCRYPT_PLACEHOLDER',
    'ADMIN'
)
ON CONFLICT (email) DO NOTHING;
