-- Table: shipment
-- Purpose: Represents a shipment created by a user (e.g., an order with multiple items)

CREATE TABLE IF NOT EXISTS shipment (
                          id BIGSERIAL PRIMARY KEY,
                          shipment_number VARCHAR(50) NOT NULL UNIQUE,
                          user_id BIGINT NOT NULL,
                          origin VARCHAR(255) NOT NULL,
                          destination VARCHAR(255) NOT NULL,
                          departure_date TIMESTAMP,
                          arrival_date TIMESTAMP,
                          status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_shipment_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE RESTRICT
);

CREATE INDEX idx_shipments_number ON shipment(shipment_number);
CREATE INDEX idx_shipment_user ON shipment(user_id);
CREATE INDEX idx_shipment_status ON shipment(status);
CREATE INDEX idx_shipments_departure ON shipment(departure_date);
