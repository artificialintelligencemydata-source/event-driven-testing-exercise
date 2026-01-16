package com.bjs.tests.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * AUTWIT Client Test Application.
 * 
 * CRITICAL ARCHITECTURE:
 * - DataSourceAutoConfiguration is excluded: Adapters own DataSource for SQL adapters
 * - HibernateJpaAutoConfiguration is excluded: Prevents JPA from requiring DataSource when Mongo is active
 * 
 * When autwit.database=mongo: No DataSource/JPA needed (Mongo uses MongoClient)
 * When autwit.database=postgres|h2: Adapter creates DataSource and configures JPA
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.acuver.autwit",   // AUTWIT framework beans
                "com.bjs.tests"        // Client beans (step defs, configs)
        },
        exclude = {
                DataSourceAutoConfiguration.class,      // Adapters own DataSource
                HibernateJpaAutoConfiguration.class     // Prevents JPA from requiring DataSource for Mongo
        }
)
public class ClientTestApplication {
}
