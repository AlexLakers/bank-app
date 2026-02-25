package com.alex.bank.account.repository;

import com.alex.bank.account.model.Account;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends CrudRepository<Account, Long> {

    Optional<Account> findAccountByUsername(String username);

    List<Account> findAccountsByUsernameNot(String username);




}
