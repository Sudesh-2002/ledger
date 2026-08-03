package com.sudesh.ledger.query;

import com.sudesh.ledger.command.domain.command.DepositCommand;
import com.sudesh.ledger.command.domain.command.OpenAccountCommand;
import com.sudesh.ledger.command.service.AccountCommandService;
import com.sudesh.ledger.query.projection.AccountProjectionRebuildService;
import com.sudesh.ledger.query.repository.AccountSummaryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest
class AccountProjectionIntegrationTest {

    @Autowired private AccountCommandService commandService;
    @Autowired private AccountSummaryRepository summaryRepository;
    @Autowired private AccountProjectionRebuildService rebuildService;

    @Test
    void projectionReflectsCommandsEventually() {
        String accountId = UUID.randomUUID().toString();
        commandService.openAccount(new OpenAccountCommand(accountId, "Sudesh", new BigDecimal("100.00")));
        commandService.deposit(new DepositCommand(accountId, new BigDecimal("50.00"), "ref-1"));

        await().atMost(2, SECONDS).untilAsserted(() ->
                assertThat(summaryRepository.findById(accountId)).isPresent());

        assertThat(summaryRepository.findById(accountId).get().getBalance())
                .isEqualByComparingTo("150.00");
    }

    @Test
    void rebuildReconstructsProjectionFromEventStore() {
        String accountId = UUID.randomUUID().toString();
        commandService.openAccount(new OpenAccountCommand(accountId, "Sudesh", new BigDecimal("200.00")));

        rebuildService.rebuildAll();

        assertThat(summaryRepository.findById(accountId)).isPresent();
        assertThat(summaryRepository.findById(accountId).get().getBalance())
                .isEqualByComparingTo("200.00");
    }
}