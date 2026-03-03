package com.alex.bank.cash.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder
@Table(name = "cash_transactions")
public class CashTransaction {

    @Id
    @Column("transaction_id")
    private UUID transactionId;

    @Column("account_holder")
    private String accountHolder;

    @Column("cash_action")
    private CashAction action;

    @Column("amount")
    private BigDecimal amount;

    @Column("transaction_status")
    private CashTransactionStatus status;

    @Column("message")
    private String message;

}
