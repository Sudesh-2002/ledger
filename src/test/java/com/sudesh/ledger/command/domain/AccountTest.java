package com.sudesh.ledger.command.domain;

import com.sudesh.ledger.command.domain.command.DepositCommand;
import com.sudesh.ledger.command.domain.command.OpenAccountCommand;
import com.sudesh.ledger.command.domain.command.WithdrawCommand;
import com.sudesh.ledger.command.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void opensWithInitialBalance() {
        Account account = Account.open(new OpenAccountCommand("acc-1", "Sudesh", new BigDecimal("100.00")));
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        assertThat(account.getPendingEvents()).hasSize(1);
    }

    @Test
    void depositIncreasesBalance() {
        Account account = Account.open(new OpenAccountCommand("acc-1", "Sudesh", BigDecimal.ZERO));
        account.deposit(new DepositCommand("acc-1", new BigDecimal("50.00"), "ref-1"));
        assertThat(account.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void withdrawBeyondBalanceThrows() {
        Account account = Account.open(new OpenAccountCommand("acc-1", "Sudesh", new BigDecimal("10.00")));
        assertThatThrownBy(() -> account.withdraw(new WithdrawCommand("acc-1", new BigDecimal("50.00"), "ref-1")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void replayReconstructsCorrectBalance() {
        Account fresh = Account.open(new OpenAccountCommand("acc-1", "Sudesh", new BigDecimal("100.00")));
        fresh.deposit(new DepositCommand("acc-1", new BigDecimal("25.00"), "ref-1"));
        fresh.withdraw(new WithdrawCommand("acc-1", new BigDecimal("40.00"), "ref-2"));

        List<Object> history = fresh.getPendingEvents();
        Account rebuilt = Account.replay(history);

        assertThat(rebuilt.getBalance()).isEqualByComparingTo("85.00");
        assertThat(rebuilt.getVersion()).isEqualTo(3);
    }
}