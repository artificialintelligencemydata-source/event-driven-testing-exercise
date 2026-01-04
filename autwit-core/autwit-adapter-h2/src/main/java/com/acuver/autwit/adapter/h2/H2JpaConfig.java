package com.acuver.autwit.adapter.h2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnProperty(name = "autwit.database", havingValue = "h2")
@EnableJpaRepositories(basePackages = "com.acuver.autwit.adapter.h2")
@EntityScan(basePackages = "com.acuver.autwit.adapter.h2")
public class H2JpaConfig {}
