package com.alex.bank.account.integration.repository;

import com.alex.bank.account.config.PostgresTestconteinerConfig;
import com.alex.bank.account.model.Account;
import com.alex.bank.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@ImportTestcontainers(PostgresTestconteinerConfig.class)
@Sql(scripts = "/sql/data-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class AccountRepositoryIT {

    @Autowired
    private AccountRepository accountRepository;

    private final String username = "testov1";

    @Test
    void findAccountByUsername_shouldReturnAccount() {
        Optional<Account> account = accountRepository.findAccountByUsername(username);
        assertThat(account).isPresent();
        assertThat(account.get().getUsername()).isEqualTo(username);
    }

    @Test
    void findAccountByUsername_shouldReturnEmptyWhenNotFound() {
        Optional<Account> account = accountRepository.findAccountByUsername("nonexistent");
        assertThat(account).isEmpty();
    }

    @Test
    void existsByUsername_shouldReturnTrueForExisting() {
        boolean exists = accountRepository.existsByUsername(username);
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_shouldReturnFalseForNonExisting() {
        boolean exists = accountRepository.existsByUsername("nonexistent");
        assertThat(exists).isFalse();
    }

    @Test
    void increaseBalanceByUsername_shouldIncreaseBalance() {
        BigDecimal amount = new BigDecimal("200.00");

        Optional<BigDecimal> updated = accountRepository.increaseBalanceByUsername(username, amount);
        assertThat(updated).isPresent();

        Optional<Account> updatedAccount = accountRepository.findAccountByUsername(username);
        assertThat(updatedAccount).isPresent();
        assertThat(updatedAccount.get().getBalance()).isEqualByComparingTo("1200.00");
    }

    @Test
    void increaseBalanceByUsername_shouldReturnEmptyWhenUserNotFound() {
        BigDecimal amount = new BigDecimal("200.00");

        Optional<BigDecimal> updated = accountRepository.increaseBalanceByUsername("nonexistent", amount);
        assertThat(updated).isEmpty();
    }

    @Test
    void decreaseBalanceByUsername_shouldDecreaseWhenSufficientFunds() {
        BigDecimal amount = new BigDecimal("300.00");

        Optional<BigDecimal> updated = accountRepository.decreaseBalanceByUsername(username, amount);
        assertThat(updated).isPresent();

        Optional<Account> updatedAccount = accountRepository.findAccountByUsername(username);
        assertThat(updatedAccount).isPresent();
        assertThat(updatedAccount.get().getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void decreaseBalanceByUsername_shouldReturnEmptyWhenInsufficientFunds() {
        BigDecimal amount = new BigDecimal("2000.00");

        Optional<BigDecimal> updated = accountRepository.decreaseBalanceByUsername(username, amount);
        assertThat(updated).isEmpty();

        Optional<Account> unchanged = accountRepository.findAccountByUsername(username);
        assertThat(unchanged).isPresent();
        assertThat(unchanged.get().getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void decreaseBalanceByUsername_shouldReturnEmptyWhenUserNotFound() {
        BigDecimal amount = new BigDecimal("100.00");

        Optional<BigDecimal> updated = accountRepository.decreaseBalanceByUsername("nonexistent", amount);
        assertThat(updated).isEmpty();
    }
}
