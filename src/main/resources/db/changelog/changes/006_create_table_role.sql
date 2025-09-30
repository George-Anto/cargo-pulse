-- Role table
CREATE TABLE IF NOT EXISTS role (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(50) NOT NULL UNIQUE
);