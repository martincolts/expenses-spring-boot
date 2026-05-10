package com.tincho.expenses.services;

import com.tincho.expenses.controllers.dto.CreateCategoryRequest;
import com.tincho.expenses.controllers.dto.UpdateCategoryRequest;
import com.tincho.expenses.model.Category;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.CategoryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;

    @Transactional
    public Category create(Long tenantId, CreateCategoryRequest req) {
        if (repository.existsByTenantIdAndName(tenantId, req.name())) {
            throw new CategoryNameAlreadyExistsException(req.name());
        }
        Category c = new Category(
                null,
                AggregateReference.<Tenant, Long>to(tenantId),
                req.name(),
                OffsetDateTime.now()
        );
        return repository.save(c);
    }

    public List<Category> listForTenant(Long tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public List<Category> listAll() {
        return repository.findAll();
    }

    public Category getForTenant(Long tenantId, Long id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public Category get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Transactional
    public Category update(Long tenantId, Long id, UpdateCategoryRequest req) {
        Category existing = getForTenant(tenantId, id);
        if (!existing.name().equals(req.name())
                && repository.existsByTenantIdAndName(tenantId, req.name())) {
            throw new CategoryNameAlreadyExistsException(req.name());
        }
        Category updated = new Category(
                existing.id(),
                existing.tenant(),
                req.name(),
                existing.createdAt()
        );
        return repository.save(updated);
    }

    @Transactional
    public void deleteForTenant(Long tenantId, Long id) {
        Category existing = getForTenant(tenantId, id);
        repository.deleteById(existing.id());
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
