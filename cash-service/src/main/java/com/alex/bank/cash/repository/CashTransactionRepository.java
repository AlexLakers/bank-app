package com.alex.bank.cash.repository;

import com.alex.bank.cash.model.CashTransaction;
import org.springframework.data.repository.CrudRepository;

public interface CashTransactionRepository extends CrudRepository<CashTransaction, String> {
}
