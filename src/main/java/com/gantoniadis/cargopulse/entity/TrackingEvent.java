package com.gantoniadis.cargopulse.entity;

import com.gantoniadis.cargopulse.util.TrackingEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_event",
        indexes = {
                @Index(name = "idx_tracking_event_shipment", columnList = "shipment_id"),
                @Index(name = "idx_tracking_event_type", columnList = "event_type")
        })
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString(exclude = "shipment")
@EqualsAndHashCode(exclude = "shipment")
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tracking_event_shipment"))
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private TrackingEventType eventType;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @PrePersist
    protected void onCreate() {
        this.eventTimestamp = LocalDateTime.now();
    }
}
