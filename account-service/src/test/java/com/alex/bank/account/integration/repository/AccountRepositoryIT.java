package com.alex.bank.account.integration.repository;

import com.alex.bank.account.config.PostgresTestconteinerConfig;
import com.alex.bank.account.model.Account;
import com.alex.bank.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ImportTestcontainers(PostgresTestconteinerConfig.class)
@Sql(scripts = "/sql/data-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class AccountRepositoryIT {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findAccountByUsername_shouldReturnAccount() {
        Optional<Account> account = accountRepository.findAccountByUsername("testov1");
        assertThat(account).isPresent();
        assertThat(account.get().getUsername()).isEqualTo("testov1");
        assertThat(account.get().getName()).isEqualTo("Testov Test");
        assertThat(account.get().getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void findAccountByUsername_shouldReturnEmptyWhenNotFound() {
        Optional<Account> account = accountRepository.findAccountByUsername("nonexistent");
        assertThat(account).isEmpty();
    }


    @Test
    @Transactional
    void increaseBalanceByUsername_shouldIncreaseBalance() {
        String username = "testov1";
        BigDecimal initial = accountRepository.findAccountByUsername(username).get().getBalance();
        BigDecimal amount = new BigDecimal("200.00");

        Long updated = accountRepository.increaseBalanceByUsername(username, amount);
        assertThat(updated).isEqualTo(1);

        Optional<Account> updatedAccount = accountRepository.findAccountByUsername(username);
        assertThat(updatedAccount).isPresent();
        assertThat(updatedAccount.get().getBalance()).isEqualByComparingTo(initial.add(amount));
    }

    @Test
    @Transactional
    void decreaseBalanceByUsername_shouldDecreaseWhenSufficientFunds() {
        String username = "testov1";
        BigDecimal initial = accountRepository.findAccountByUsername(username).get().getBalance();
        BigDecimal amount = new BigDecimal("300.00");

        Long updated = accountRepository.decreaseBalanceByUsername(username, amount);
        assertThat(updated).isEqualTo(1);

        Optional<Account> updatedAccount = accountRepository.findAccountByUsername(username);
        assertThat(updatedAccount).isPresent();
        assertThat(updatedAccount.get().getBalance()).isEqualByComparingTo(initial.subtract(amount));
    }

    @Test
    @Transactional
    void decreaseBalanceByUsername_shouldNotChangeWhenInsufficientFunds() {
        String username = "testov1";
        BigDecimal initial = accountRepository.findAccountByUsername(username).get().getBalance();
        BigDecimal amount = initial.add(new BigDecimal("100.00"));

        Long updated = accountRepository.decreaseBalanceByUsername(username, amount);
        assertThat(updated).isZero();

        Optional<Account> unchanged = accountRepository.findAccountByUsername(username);
        assertThat(unchanged).isPresent();
        assertThat(unchanged.get().getBalance()).isEqualByComparingTo(initial);
    }
}
