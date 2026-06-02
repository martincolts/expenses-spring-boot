package com.tincho.expenses.services;

import com.tincho.expenses.controllers.dto.CreateIncomeRequest;
import com.tincho.expenses.controllers.dto.UpdateIncomeRequest;
import com.tincho.expenses.exceptions.IncomeNotFoundException;
import com.tincho.expenses.model.Income;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.IncomeRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository repository;

    @Transactional
    public Income create(Long tenantId, CreateIncomeRequest req) {
        Income income = new Income(
                null,
                AggregateReference.<Tenant, Long>to(tenantId),
                req.amount(),
                OffsetDateTime.now()
        );
        return repository.save(income);
    }

    public List<Income> listForTenant(Long tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public Page<Income> listForTenant(Long tenantId, Pageable pageable) {
        List<Income> content = repository.findByTenantIdPaged(tenantId, pageable.getPageSize(), pageable.getOffset());
        long total = repository.countByTenantId(tenantId);
        return new PageImpl<>(content, pageable, total);
    }

    public List<Income> listAll() {
        return repository.findAll();
    }

    public Page<Income> listAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Income getForTenant(Long tenantId, Long id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IncomeNotFoundException(id));
    }

    public Income get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IncomeNotFoundException(id));
    }

    @Transactional
    public Income update(Long tenantId, Long id, UpdateIncomeRequest req) {
        Income existing = getForTenant(tenantId, id);
        return repository.save(new Income(existing.id(), existing.tenant(), req.amount(), existing.createdAt()));
    }

    @Transactional
    public void deleteForTenant(Long tenantId, Long id) {
        Income existing = getForTenant(tenantId, id);
        repository.deleteById(existing.id());
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IncomeNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
