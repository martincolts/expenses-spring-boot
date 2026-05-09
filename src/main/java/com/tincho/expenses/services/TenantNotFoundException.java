package com.tincho.expenses.services;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(Long id) {
        super("Tenant " + id + " not found");
    }
}
