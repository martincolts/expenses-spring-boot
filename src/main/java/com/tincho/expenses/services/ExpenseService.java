package com.tincho.expenses.services;

import com.tincho.expenses.controllers.dto.CreateExpenseRequest;
import com.tincho.expenses.controllers.dto.UpdateExpenseRequest;
import com.tincho.expenses.exceptions.CategoryNotFoundException;
import com.tincho.expenses.exceptions.ExpenseAlreadyPaidException;
import com.tincho.expenses.exceptions.ExpenseNotFoundException;
import com.tincho.expenses.exceptions.ExpenseNotPaidException;
import com.tincho.expenses.model.Category;
import com.tincho.expenses.model.Expense;
import com.tincho.expenses.model.ExpenseStatus;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.CategoryRepository;
import com.tincho.expenses.repositories.ExpenseRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository repository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Expense create(Long tenantId, CreateExpenseRequest req) {
        requireCategoryForTenant(tenantId, req.categoryId());
        OffsetDateTime now = OffsetDateTime.now();
        ExpenseStatus status = req.paid() ? ExpenseStatus.PAID : ExpenseStatus.NOT_PAUD_YET;
        OffsetDateTime paidAt = req.paid() ? now : null;
        Expense expense = new Expense(
                null,
                AggregateReference.<Tenant, Long>to(tenantId),
                AggregateReference.<Category, Long>to(req.categoryId()),
                req.description(),
                req.amount(),
                req.currency().toUpperCase(),
                status,
                req.incurredAt(),
                now,
                paidAt
        );
        return repository.save(expense);
    }

    public List<Expense> listForTenant(Long tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public List<Expense> listAll() {
        return repository.findAll();
    }

    public Expense getForTenant(Long tenantId, Long id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    public Expense get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    @Transactional
    public Expense update(Long tenantId, Long id, UpdateExpenseRequest req) {
        Expense existing = getForTenant(tenantId, id);
        requireCategoryForTenant(tenantId, req.categoryId());
        Expense updated = new Expense(
                existing.id(),
                existing.tenant(),
                AggregateReference.<Category, Long>to(req.categoryId()),
                req.description(),
                req.amount(),
                req.currency().toUpperCase(),
                existing.status(),
                req.incurredAt(),
                existing.createdAt(),
                existing.paidAt()
        );
        return repository.save(updated);
    }

    @Transactional
    public Expense markPaidForTenant(Long tenantId, Long id) {
        return markPaid(getForTenant(tenantId, id));
    }

    @Transactional
    public Expense markPaid(Long id) {
        return markPaid(get(id));
    }

    @Transactional
    public Expense markUnpaidForTenant(Long tenantId, Long id) {
        return markUnpaid(getForTenant(tenantId, id));
    }

    @Transactional
    public Expense markUnpaid(Long id) {
        return markUnpaid(get(id));
    }

    @Transactional
    public void deleteForTenant(Long tenantId, Long id) {
        Expense existing = getForTenant(tenantId, id);
        repository.deleteById(existing.id());
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ExpenseNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private Expense markPaid(Expense existing) {
        if (existing.status() == ExpenseStatus.PAID) {
            throw new ExpenseAlreadyPaidException(existing.id());
        }
        Expense paid = new Expense(
                existing.id(),
                existing.tenant(),
                existing.category(),
                existing.description(),
                existing.amount(),
                existing.currency(),
                ExpenseStatus.PAID,
                existing.incurredAt(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        return repository.save(paid);
    }

    private Expense markUnpaid(Expense existing) {
        if (existing.status() != ExpenseStatus.PAID) {
            throw new ExpenseNotPaidException(existing.id());
        }
        Expense unpaid = new Expense(
                existing.id(),
                existing.tenant(),
                existing.category(),
                existing.description(),
                existing.amount(),
                existing.currency(),
                ExpenseStatus.NOT_PAUD_YET,
                existing.incurredAt(),
                existing.createdAt(),
                null
        );
        return repository.save(unpaid);
    }

    private void requireCategoryForTenant(Long tenantId, Long categoryId) {
        if (categoryRepository.findByIdAndTenantId(categoryId, tenantId).isEmpty()) {
            throw new CategoryNotFoundException(categoryId);
        }
    }
}
