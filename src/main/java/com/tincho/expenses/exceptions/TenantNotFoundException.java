package com.tincho.expenses.exceptions;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(Long id) {
        super("Tenant " + id + " not found");
    }
}
