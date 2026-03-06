package model;


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
@Table("transfer_transactions")
public class TransferTransaction {
    @Id
    @Column("transaction_id")
    private UUID transactionId;

    @Column("from_account")
    private String fromAccount;

    @Column("to_account")
    private String toAccount;

    @Column("amount")
    private BigDecimal amount;

    @Column("message")
    private String message;

    @Column("transaction_status")
    private TransferTransactionStatus status;
}
