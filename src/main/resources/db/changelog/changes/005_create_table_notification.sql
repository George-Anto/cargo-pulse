-- Table: notification
-- Purpose: Records notifications sent to users (e.g., email, SMS, push)

CREATE TABLE IF NOT EXISTS notification (
                              id BIGSERIAL PRIMARY KEY,
                              user_id BIGINT NOT NULL,
                              tracking_event_id BIGINT NOT NULL,
                              channel VARCHAR(50) NOT NULL, -- e.g., EMAIL, SMS
                              status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- e.g., PENDING, SENT, FAILED
                              sent_at TIMESTAMP,

                              CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE RESTRICT,
                              CONSTRAINT fk_notification_event FOREIGN KEY (tracking_event_id) REFERENCES tracking_event(id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_user ON notification(user_id);
CREATE INDEX idx_notification_status ON notification(status);
