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

    @Query("UPDATE accounts SET balance = balance - :amount WHERE username = :username AND balance >= :amount RETURNING balance")
    Optional<BigDecimal> decreaseBalanceByUsername(String username, BigDecimal amount);

    @Query("UPDATE accounts SET balance = balance + :amount WHERE username = :username RETURNING balance")
    @Modifying
    Optional<BigDecimal> increaseBalanceByUsername(String username, BigDecimal amount);

    boolean existsByUsername(String username);


}
