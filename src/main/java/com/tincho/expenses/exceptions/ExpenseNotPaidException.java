package com.tincho.expenses.exceptions;

public class ExpenseNotPaidException extends RuntimeException {

    public ExpenseNotPaidException(Long id) {
        super("Expense " + id + " is not paid");
    }
}
