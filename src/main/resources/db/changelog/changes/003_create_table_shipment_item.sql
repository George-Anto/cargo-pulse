-- Table: shipment_item
-- Purpose: Represents an individual item inside a shipment (e.g., a phone in an order)

CREATE TABLE IF NOT EXISTS shipment_item (
                               id BIGSERIAL PRIMARY KEY,
                               item_number VARCHAR(50) NOT NULL UNIQUE,
                               shipment_id BIGINT NOT NULL,
                               description VARCHAR(255) NOT NULL,
                               weight DECIMAL(10,2),
                               value DECIMAL(12,2),
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_shipment_item_shipment FOREIGN KEY (shipment_id) REFERENCES shipment(id) ON DELETE CASCADE
);

CREATE INDEX idx_shipment_item_number ON shipment_item(item_number);
CREATE INDEX idx_shipment_item_id ON shipment_item(shipment_id);
