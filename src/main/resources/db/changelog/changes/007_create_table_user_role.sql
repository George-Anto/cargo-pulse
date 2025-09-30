-- Join table for users and roles
CREATE TABLE IF NOT EXISTS user_role (
                        user_id BIGINT NOT NULL,
                        role_id BIGINT NOT NULL,

                        PRIMARY KEY(user_id, role_id),
                        CONSTRAINT fk_user_role_user FOREIGN KEY(user_id) REFERENCES user_account(id) ON DELETE CASCADE,
                        CONSTRAINT fk_user_role_role FOREIGN KEY(role_id) REFERENCES role(id) ON DELETE CASCADE
);