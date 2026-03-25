package com.alex.bank.notification.model;

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
@Table(name = "events_idempotence")
public class EventIdempotence {

    @Id
    @Column("event_id")
    private String eventId;

    @Column("processed_at")
    private LocalDateTime processedAt;
}
