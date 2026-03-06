package com.alex.bank.transfer.repository;

import com.alex.bank.transfer.model.TransferTransaction;
import org.springframework.data.repository.CrudRepository;

public interface TransferTransactionRepository extends CrudRepository<TransferTransaction, String> {

}
