package com.alex.bank.transfer.model;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import com.alex.bank.common.dto.notification.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@FieldNameConstants
@Table(name = "outbox")
public class Outbox {

    @Id
    @Column("event_id")
    private UUID eventId;

    @Column("source")
    private String source;

    @Column("event_type")
    private EventType eventType;

    @Column("payload")
    private String payload;

    @Column("message")
    String message;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("processed_at")
    private LocalDateTime processedAt;
}
