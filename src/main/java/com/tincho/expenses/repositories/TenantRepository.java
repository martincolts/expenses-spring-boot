package com.tincho.expenses.repositories;

import com.tincho.expenses.model.Tenant;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends ListCrudRepository<Tenant, Long> {

    Optional<Tenant> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT * FROM tenant WHERE email = :email FOR UPDATE")
    Optional<Tenant> findByEmailForUpdate(@Param("email") String email);
}
