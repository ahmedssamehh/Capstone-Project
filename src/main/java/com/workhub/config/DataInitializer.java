package com.workhub.config;

import com.workhub.entity.Tenant;
import com.workhub.entity.User;
import com.workhub.repository.TenantRepository;
import com.workhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Data Initializer
 * 
 * Creates sample data for testing authentication
 * Remove this in production!
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Check if data already exists
            if (tenantRepository.count() > 0) {
                log.info("Data already initialized, skipping...");
                return;
            }

            log.info("Initializing test data...");

            // Create tenant
            Tenant tenant = Tenant.builder()
                    .name("Demo Company")
                    .plan(Tenant.TenantPlan.PROFESSIONAL)
                    .status(Tenant.TenantStatus.ACTIVE)
                    .maxUsers(50)
                    .build();
            tenant = tenantRepository.save(tenant);
            log.info("Created tenant: {} (ID: {})", tenant.getName(), tenant.getId());

            // Create admin user
            User admin = User.builder()
                    .tenant(tenant)
                    .email("admin@demo.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("User")
                    .role(User.UserRole.TENANT_ADMIN)
                    .status(User.UserStatus.ACTIVE)
                    .build();
            admin = userRepository.save(admin);
            log.info("Created admin user: {} (ID: {})", admin.getEmail(), admin.getId());

            // Create regular user
            User user = User.builder()
                    .tenant(tenant)
                    .email("user@demo.com")
                    .password(passwordEncoder.encode("user123"))
                    .firstName("Regular")
                    .lastName("User")
                    .role(User.UserRole.TENANT_USER)
                    .status(User.UserStatus.ACTIVE)
                    .build();
            user = userRepository.save(user);
            log.info("Created regular user: {} (ID: {})", user.getEmail(), user.getId());

            log.info("Test data initialization complete!");
            log.info("You can now login with:");
            log.info("  Admin: admin@demo.com / admin123");
            log.info("  User:  user@demo.com / user123");
        };
    }
}
