package com.acuver.autwit.adapter.postgres;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnProperty(name = "autwit.database", havingValue = "postgres")
@EnableJpaRepositories(basePackages = "com.acuver.autwit.adapter.postgres")
@EntityScan(basePackages = "com.acuver.autwit.adapter.postgres")
public class PostgresJpaConfig {}

