package com.alex.bank.notification.entity;

import com.alex.bank.common.dto.notification.EventType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@FieldNameConstants
@Table(name = "events")
public class Event {

    @Id
    @Column("event_id")
    private String eventId;

    @Column("created_at")
    private LocalDateTime processedAt;
}
