package com.sudesh.ledger.command.api;

import com.sudesh.ledger.command.api.dto.DepositRequest;
import com.sudesh.ledger.command.api.dto.OpenAccountRequest;
import com.sudesh.ledger.command.api.dto.WithdrawRequest;
import com.sudesh.ledger.command.domain.command.DepositCommand;
import com.sudesh.ledger.command.domain.command.OpenAccountCommand;
import com.sudesh.ledger.command.domain.command.WithdrawCommand;
import com.sudesh.ledger.command.service.AccountCommandService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountCommandService commandService;

    public AccountController(AccountCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    public ResponseEntity<Void> open(@Valid @RequestBody OpenAccountRequest request) {
        commandService.openAccount(new OpenAccountCommand(
                request.accountId(), request.ownerName(), request.openingBalance()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<Void> deposit(@PathVariable String accountId,
                                         @Valid @RequestBody DepositRequest request) {
        commandService.deposit(new DepositCommand(accountId, request.amount(), request.reference()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable String accountId,
                                          @Valid @RequestBody WithdrawRequest request) {
        commandService.withdraw(new WithdrawCommand(accountId, request.amount(), request.reference()));
        return ResponseEntity.accepted().build();
    }
}