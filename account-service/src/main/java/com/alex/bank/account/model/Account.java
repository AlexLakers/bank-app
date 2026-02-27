package com.alex.bank.account.model;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
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
@Table(name = "accounts")
public class Account {
    @Id
    private Long id;

    @Column("username")
    private String username;

    @Column("name")
    private String name;


    @Column("birth_date")
    private LocalDate birthdate;

    @Column("balance")
    private BigDecimal balance;


}
