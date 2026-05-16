-- Global user identity hardening: enforce unique email across tenants.
-- Eliminates authentication ambiguity for loadUserByUsername(email).

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_user_email'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT uk_user_email UNIQUE (email);
    END IF;
END $$;
