package com.tincho.expenses.repositories;

import com.tincho.expenses.model.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends ListCrudRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("UPDATE password_reset_token SET used_at = now() WHERE tenant_id = :tenantId AND used_at IS NULL")
    void invalidateActiveForTenant(@Param("tenantId") Long tenantId);
}
