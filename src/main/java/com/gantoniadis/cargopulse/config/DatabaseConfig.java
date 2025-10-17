package com.gantoniadis.cargopulse.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import javax.sql.DataSource;

/**
 * Explicitly defines the DataSource bean and enforces a dependency on
 * the AzureKeyVaultConfig to ensure all required System Properties
 * are loaded before the connection pool is initialized.
 */
@Configuration
public class DatabaseConfig {

    /**
     * Re-defines the standard Spring Boot DataSource bean.
     * * @param properties The autoconfigured properties from application.yml
     * @return The configured DataSource (HikariPool)
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    @DependsOn("azureKeyVaultConfig")
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}