package com.recetea.infrastructure.persistence.recipe.jdbc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Infrastructure singleton that manages the HikariCP connection pool.
 * Resolves the properties file from the classpath using the {@code env} system property
 * (defaults to {@code "local"}, producing {@code application-local.properties}).
 */
public class DatabaseConfig {

    private static volatile DataSource dataSource;

    private DatabaseConfig() {}

    /**
     * Returns the shared {@link DataSource}, initialising it on first access
     * via double-checked locking.
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    dataSource = buildDataSource();
                }
            }
        }
        return dataSource;
    }

    private static DataSource buildDataSource() {
        Properties properties = new Properties();
        String env = System.getProperty("env", "local");
        String fileName = "application-" + env + ".properties";

        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                throw new IllegalStateException("Config file not found on classpath: " + fileName);
            }
            properties.load(input);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(properties.getProperty("db.url"));
            config.setUsername(properties.getProperty("db.user"));
            config.setPassword(properties.getProperty("db.password"));

            // Pool resilience settings
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            return new HikariDataSource(config);

        } catch (IOException e) {
            throw new RuntimeException("I/O failure loading database configuration.", e);
        }
    }
}
