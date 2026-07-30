package com.sudesh.ledger.command.domain.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String accountId) {
        super("Insufficient funds in account " + accountId);
    }
}