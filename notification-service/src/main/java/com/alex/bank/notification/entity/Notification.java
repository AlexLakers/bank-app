package com.alex.bank.notification.entity;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@FieldNameConstants
@Table(name = "notifications")
public class Notification {

    @Id
    @Column("notification_id")
    private String notificationId;

    @Column("processed_at")
    private LocalDateTime processedAt;
}
