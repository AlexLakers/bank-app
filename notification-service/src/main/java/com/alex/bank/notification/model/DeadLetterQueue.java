package com.alex.bank.notification.model;


import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@FieldNameConstants
@Table(name = "dead_letter_queue")
public class DeadLetterQueue {

    @Id
    @Column("id")
    private Long id;

    @Column("msg_topic")
    private String msgTopic;

    @Column("msg_partition")
    private Integer msgPartition;

    @Column("msg_offset")
    private Long msgOffset;

    @Column("msg_key")
    private String msgKey;

    @Column("error_message")
    private String errorMessage;


}