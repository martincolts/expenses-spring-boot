package com.tincho.expenses.services;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tincho.expenses.controllers.dto.CreateCategoryRequest;
import com.tincho.expenses.controllers.dto.UpdateCategoryRequest;
import com.tincho.expenses.exceptions.CategoryNameAlreadyExistsException;
import com.tincho.expenses.exceptions.CategoryNotFoundException;
import com.tincho.expenses.model.Category;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.CategoryRepository;

import lombok.RequiredArgsConstructor;

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
                req.iconId(),
                req.colorId(),
                OffsetDateTime.now()
        );
        return repository.save(c);
    }

    public List<Category> listForTenant(Long tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public Page<Category> listForTenant(Long tenantId, Pageable pageable) {
        List<Category> content = repository.findByTenantIdPaged(tenantId, pageable.getPageSize(), pageable.getOffset());
        long total = repository.countByTenantId(tenantId);
        return new PageImpl<>(content, pageable, total);
    }

    public List<Category> listAll() {
        return repository.findAll();
    }

    public Page<Category> listAll(Pageable pageable) {
        return repository.findAll(pageable);
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
                req.iconId(),
                req.colorId(),
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
