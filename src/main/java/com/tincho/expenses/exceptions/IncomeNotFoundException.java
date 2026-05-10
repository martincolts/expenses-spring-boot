package com.tincho.expenses.exceptions;

public class IncomeNotFoundException extends RuntimeException {

    public IncomeNotFoundException(Long id) {
        super("Income " + id + " not found");
    }
}
