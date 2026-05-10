package com.tincho.expenses.exceptions;

public class TenantEmailAlreadyExistsException extends RuntimeException {

    public TenantEmailAlreadyExistsException(String email) {
        super("Tenant with email '" + email + "' already exists");
    }
}
