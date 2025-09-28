-- Table: tracking_event
-- Purpose: Stores tracking updates for shipment (e.g., dispatched, in-transit, delivered)

CREATE TABLE IF NOT EXISTS tracking_event (
                                id BIGSERIAL PRIMARY KEY,
                                shipment_id BIGINT NOT NULL,
                                event_type VARCHAR(100) NOT NULL, -- e.g., DISPATCHED, IN_TRANSIT, DELIVERED
                                event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                location VARCHAR(255),
                                description TEXT,

                                CONSTRAINT fk_tracking_event_shipment FOREIGN KEY (shipment_id) REFERENCES shipment(id) ON DELETE CASCADE
);

CREATE INDEX idx_tracking_event_shipment ON tracking_event(shipment_id);
CREATE INDEX idx_tracking_event_type ON tracking_event(event_type);
