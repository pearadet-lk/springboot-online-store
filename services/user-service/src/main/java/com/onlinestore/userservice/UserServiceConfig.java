package com.onlinestore.userservice;

import com.onlinestore.userservice.persistence.RefreshTokenJpaRepository;
import com.onlinestore.userservice.persistence.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class UserServiceConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserStore userStore() {
        return new UserStore();
    }

    @Bean
    public JwtTokenService jwtTokenService(AuthProperties authProperties) {
        return new JwtTokenService(authProperties);
    }

    @Bean
    public UserAccountService userAccountService(
            UserPersistenceMode mode,
            UserStore memoryStore,
            @Autowired(required = false) UserJpaRepository userRepo,
            @Autowired(required = false) RefreshTokenJpaRepository refreshRepo,
            PasswordEncoder passwordEncoder) {
        return new UserAccountService(mode, memoryStore, userRepo, refreshRepo, passwordEncoder);
    }
}
