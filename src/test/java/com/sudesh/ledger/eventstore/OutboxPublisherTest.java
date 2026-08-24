package com.sudesh.ledger.eventstore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest
class OutboxPublisherTest {

    @Autowired private OutboxRepository outboxRepository;

    @Test
    void allOutboxEntriesEventuallyMarkedPublished() {
        // relies on prior test activity or manually inserted rows having created outbox entries;
        // in isolation you'd fire a real command here via AccountCommandService first
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findTop100ByPublishedFalseOrderByIdAsc()).isEmpty());
    }
}