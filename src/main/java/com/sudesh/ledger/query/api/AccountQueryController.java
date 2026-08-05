package com.sudesh.ledger.query.api;

import com.sudesh.ledger.query.api.dto.AccountSummaryResponse;
import com.sudesh.ledger.query.api.dto.TransactionHistoryResponse;
import com.sudesh.ledger.query.repository.AccountSummaryRepository;
import com.sudesh.ledger.query.repository.TransactionHistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountQueryController {

    private final AccountSummaryRepository summaryRepository;
    private final TransactionHistoryRepository historyRepository;

    public AccountQueryController(AccountSummaryRepository summaryRepository,
                                   TransactionHistoryRepository historyRepository) {
        this.summaryRepository = summaryRepository;
        this.historyRepository = historyRepository;
    }

    @GetMapping("/{accountId}/summary")
    public ResponseEntity<AccountSummaryResponse> summary(@PathVariable String accountId) {
        return summaryRepository.findById(accountId)
                .map(s -> new AccountSummaryResponse(s.getAccountId(), s.getOwnerName(), s.getBalance(), s.getStatus()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/transactions")
    public List<TransactionHistoryResponse> transactions(@PathVariable String accountId) {
        return historyRepository.findByAccountIdOrderByOccurredAtAsc(accountId).stream()
                .map(t -> new TransactionHistoryResponse(t.getType(), t.getAmount(), t.getReference(),
                        t.getBalanceAfter(), t.getOccurredAt()))
                .toList();
    }
}