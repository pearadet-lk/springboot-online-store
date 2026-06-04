package com.onlinestore.userservice;

import com.onlinestore.contracts.DemoConstants;
import com.onlinestore.userservice.persistence.UserEntity;
import com.onlinestore.userservice.persistence.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Configuration
public class UserPersistenceBootstrap {

    private static final Logger log = LoggerFactory.getLogger(UserPersistenceBootstrap.class);

    @Bean
    UserPersistenceMode userPersistenceMode(Environment environment) {
        if (environment.acceptsProfiles("postgres")) {
            log.info("User auth store using PostgreSQL (postgres profile).");
            return UserPersistenceMode.POSTGRES;
        }
        log.warn("User auth store using in-memory fallback (no postgres profile).");
        return UserPersistenceMode.MEMORY;
    }

    @Bean
    ApplicationRunner demoUserSeeder(
            UserPersistenceMode mode,
            @org.springframework.beans.factory.annotation.Autowired(required = false) UserJpaRepository userRepo,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (mode != UserPersistenceMode.POSTGRES) {
                return;
            }
            if (userRepo.existsById(DemoConstants.DEMO_USER_ID)) {
                return;
            }
            var entity = new UserEntity();
            entity.setUserId(DemoConstants.DEMO_USER_ID);
            entity.setEmail(DemoConstants.DEMO_EMAIL);
            entity.setPasswordHash(passwordEncoder.encode(DemoConstants.DEMO_PASSWORD));
            entity.setFullName(DemoConstants.DEMO_FULL_NAME);
            entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            userRepo.save(entity);
        };
    }
}
