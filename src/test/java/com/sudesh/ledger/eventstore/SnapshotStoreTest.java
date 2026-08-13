package com.sudesh.ledger.eventstore;

import com.sudesh.ledger.command.domain.Account;
import com.sudesh.ledger.command.domain.command.DepositCommand;
import com.sudesh.ledger.command.domain.command.OpenAccountCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotStoreTest {

    @Test
    void restoreFromSnapshotMatchesFullReplay() {
        Account fresh = Account.open(new OpenAccountCommand("acc-1", "Sudesh", new BigDecimal("100.00")));
        fresh.deposit(new DepositCommand("acc-1", new BigDecimal("10.00"), "r1"));
        fresh.deposit(new DepositCommand("acc-1", new BigDecimal("10.00"), "r2"));

        // simulate: snapshot taken right after the first two events (version 2)
        AccountSnapshot snapshot = new AccountSnapshot("acc-1", 2, "Sudesh",
                new BigDecimal("110.00"), "OPEN");

        List<Object> allEvents = fresh.getPendingEvents(); // 3 events total
        List<Object> eventsSinceSnapshot = allEvents.subList(2, allEvents.size()); // just the 2nd deposit

        Account viaSnapshot = Account.restoreFromSnapshot(snapshot, eventsSinceSnapshot);
        Account viaFullReplay = Account.replay(allEvents);

        assertThat(viaSnapshot.getBalance()).isEqualByComparingTo(viaFullReplay.getBalance());
        assertThat(viaSnapshot.getVersion()).isEqualTo(viaFullReplay.getVersion());
    }
}