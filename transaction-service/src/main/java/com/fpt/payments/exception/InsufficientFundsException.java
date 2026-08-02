package com.fpt.payments.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) { super(message); }
}
