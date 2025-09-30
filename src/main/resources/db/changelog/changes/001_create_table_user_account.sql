-- Table: user_account
-- Purpose: Stores all registered users of CargoPulse

CREATE TABLE IF NOT EXISTS user_account (
                              id BIGSERIAL PRIMARY KEY,
                              username VARCHAR(50) NOT NULL UNIQUE,
                              password VARCHAR(255) NOT NULL,
                              full_name VARCHAR(100),
                              email VARCHAR(100) UNIQUE,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_account_username ON user_account(username);
CREATE INDEX idx_user_account_email ON user_account(email);
