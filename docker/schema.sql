-- Chronos PostgreSQL Schema (Multi-User)
-- Run with: psql -d chronos -f schema.sql

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'DEVELOPER')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tasks table (user-scoped)
CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(50) NOT NULL DEFAULT 'LOW',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP,
    completed_at TIMESTAMP,
    completion_time_minutes INTEGER DEFAULT 0,
    assigned_user_id BIGINT REFERENCES users(id),
    category_id BIGINT,
    store_sessions BOOLEAN DEFAULT TRUE,
    tags TEXT[] DEFAULT '{}'
);

-- Pomodoros table (user-scoped)
CREATE TABLE IF NOT EXISTS pomodoros (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    task_name VARCHAR(255) NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 25,
    completed_sessions INTEGER DEFAULT 0,
    last_session_date TIMESTAMP,
    is_active BOOLEAN DEFAULT FALSE,
    started_at TIMESTAMP
);

-- Categories table (user-scoped)
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT
);

-- Tags table (user-scoped)
CREATE TABLE IF NOT EXISTS tags (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(50) DEFAULT '#3498DB'
);

-- Indexes for user-scoped queries
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_pomodoros_user_id ON pomodoros(user_id);
CREATE INDEX IF NOT EXISTS idx_pomodoros_task_id ON pomodoros(task_id);
CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories(user_id);
CREATE INDEX IF NOT EXISTS idx_tags_user_id ON tags(user_id);

-- Insert default users for testing
INSERT INTO users (name, email, role, created_at, updated_at) VALUES
    ('Admin User', 'admin@chronos.local', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Dev User', 'dev@chronos.local', 'DEVELOPER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Manager User', 'manager@chronos.local', 'MANAGER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;
