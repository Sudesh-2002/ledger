package com.sudesh.ledger.query;

import com.sudesh.ledger.command.domain.command.OpenAccountCommand;
import com.sudesh.ledger.command.service.AccountCommandService;
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
class AccountProjectionKafkaIntegrationTest {

    @Autowired private AccountCommandService commandService;
    @Autowired private AccountSummaryRepository summaryRepository;

    @Test
    void projectionEventuallyReflectsCommandViaKafka() {
        String accountId = UUID.randomUUID().toString();
        commandService.openAccount(new OpenAccountCommand(accountId, "Sudesh", new BigDecimal("300.00")));

        // this time the wait is real — command returns before the consumer has processed the message
        await().atMost(10, SECONDS).untilAsserted(() ->
                assertThat(summaryRepository.findById(accountId)).isPresent());

        assertThat(summaryRepository.findById(accountId).get().getBalance())
                .isEqualByComparingTo("300.00");
    }
}