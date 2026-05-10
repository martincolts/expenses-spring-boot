package com.tincho.expenses.exceptions;

public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(Long id) {
        super("Expense " + id + " not found");
    }
}
