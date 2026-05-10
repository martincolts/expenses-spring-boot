package com.tincho.expenses.services;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Category " + id + " not found");
    }
}
