package com.tincho.expenses.services;

public class CategoryNameAlreadyExistsException extends RuntimeException {

    public CategoryNameAlreadyExistsException(String name) {
        super("Category with name '" + name + "' already exists");
    }
}
