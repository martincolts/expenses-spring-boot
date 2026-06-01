package com.tincho.expenses.services;

import com.tincho.expenses.controllers.dto.CreateTenantRequest;
import com.tincho.expenses.controllers.dto.UpdateTenantRequest;
import com.tincho.expenses.exceptions.TenantEmailAlreadyExistsException;
import com.tincho.expenses.exceptions.TenantNotFoundException;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.TenantRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultCategorySeeder defaultCategorySeeder;

    @Transactional
    public Tenant create(CreateTenantRequest req) {
        if (repository.existsByEmail(req.email())) {
            throw new TenantEmailAlreadyExistsException(req.email());
        }
        Tenant tenant = new Tenant(
                null,
                req.name(),
                req.email(),
                passwordEncoder.encode(req.password()),
                "TENANT",
                OffsetDateTime.now()
        );
        Tenant saved = repository.save(tenant);
        defaultCategorySeeder.seedFor(saved.id());
        return saved;
    }

    public List<Tenant> list() {
        return repository.findAll();
    }

    public Tenant get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    @Transactional
    public Tenant update(Long id, UpdateTenantRequest req) {
        Tenant existing = get(id);
        Tenant updated = new Tenant(
                existing.id(),
                req.name(),
                existing.email(),
                existing.passwordHash(),
                existing.role(),
                existing.createdAt()
        );
        return repository.save(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new TenantNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
