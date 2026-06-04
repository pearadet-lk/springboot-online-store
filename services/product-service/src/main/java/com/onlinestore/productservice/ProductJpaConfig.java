package com.onlinestore.productservice;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("postgres")
@EnableJpaRepositories(basePackages = "com.onlinestore.productservice.persistence")
public class ProductJpaConfig {}
