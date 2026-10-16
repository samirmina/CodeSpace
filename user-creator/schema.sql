-- Chronos PostgreSQL Schema
-- Run with: psql -d chronos -f schema.sql

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'DEVELOPER')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Insert default users for testing
INSERT INTO users (name, email, role, created_at, updated_at) VALUES
    ('Admin User', 'admin@chronos.local', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Dev User', 'dev@chronos.local', 'DEVELOPER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Manager User', 'manager@chronos.local', 'MANAGER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;