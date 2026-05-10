package com.tincho.expenses.config;

import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.TenantRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (tenantRepository.existsByEmail(properties.email())) {
            log.debug("Admin tenant {} already exists; skipping bootstrap", properties.email());
            return;
        }
        Tenant admin = new Tenant(
                null,
                properties.name(),
                properties.email(),
                passwordEncoder.encode(properties.password()),
                "ADMIN",
                OffsetDateTime.now()
        );
        tenantRepository.save(admin);
        log.info("Bootstrapped admin tenant {}", properties.email());
    }
}
