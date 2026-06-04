package com.onlinestore.userservice;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("postgres")
@EnableJpaRepositories(basePackages = "com.onlinestore.userservice.persistence")
public class UserJpaConfig {}
