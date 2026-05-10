package com.tincho.expenses.exceptions;

public class ExpenseAlreadyPaidException extends RuntimeException {

    public ExpenseAlreadyPaidException(Long id) {
        super("Expense " + id + " is already paid");
    }
}
