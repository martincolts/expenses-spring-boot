package com.tincho.expenses.security;

import com.tincho.expenses.model.Tenant;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class TenantUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String role;

    public TenantUserDetails(Tenant tenant) {
        this.id = tenant.id();
        this.email = tenant.email();
        this.passwordHash = tenant.passwordHash();
        this.role = tenant.role();
    }

    // Used when reconstructing principal from JWT claims (no DB hit needed)
    public TenantUserDetails(Long id, String email, String role) {
        this.id = id;
        this.email = email;
        this.passwordHash = null;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
