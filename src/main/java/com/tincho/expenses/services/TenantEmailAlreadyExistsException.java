package com.tincho.expenses.services;

public class TenantEmailAlreadyExistsException extends RuntimeException {

    public TenantEmailAlreadyExistsException(String email) {
        super("Tenant with email '" + email + "' already exists");
    }
}
