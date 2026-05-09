package com.tincho.expenses.repositories;

import com.tincho.expenses.model.Tenant;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

public interface TenantRepository extends ListCrudRepository<Tenant, Long> {

    Optional<Tenant> findByEmail(String email);

    boolean existsByEmail(String email);
}
