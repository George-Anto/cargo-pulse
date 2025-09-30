package com.gantoniadis.cargopulse.entity;

import com.gantoniadis.cargopulse.user.entity.UserAccount;
import com.gantoniadis.cargopulse.util.NotificationChannel;
import com.gantoniadis.cargopulse.util.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification",
        indexes = {
                @Index(name = "idx_notification_user", columnList = "user_id"),
                @Index(name = "idx_notification_status", columnList = "status")
        })
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString(exclude = {"user", "trackingEvent"})
@EqualsAndHashCode(exclude = {"user", "trackingEvent"})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_user"))
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_event"))
    private TrackingEvent trackingEvent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
