package com.recetea.infrastructure.persistence.recipe.jdbc.config;

import com.recetea.core.shared.domain.utils.MaskingUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Builds the HikariCP connection pool from {@link AppConfig}. The pool is a process
 * singleton — first caller wins; subsequent calls return the same instance regardless
 * of the AppConfig passed (re-creating the pool would orphan in-flight connections).
 */
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private static volatile DataSource dataSource;

    private DatabaseConfig() {}

    /**
     * Builds (or returns the cached) HikariCP-backed DataSource. The composition root
     * and {@code BaseRepositoryTest} both call this with an explicit {@link AppConfig};
     * there is no static "load from env" shim — that hid the precedence chain and made
     * environment-variable leakage between dev/test runs hard to diagnose.
     */
    public static DataSource getDataSource(AppConfig config) {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    dataSource = buildDataSource(config);
                }
            }
        }
        return dataSource;
    }

    private static DataSource buildDataSource(AppConfig config) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(config.dbUrl());
        hc.setUsername(config.dbUser());
        hc.setPassword(config.dbPassword());

        // Cloud-DB SSL: when configured, applied as a JDBC datasource property so it
        // survives the URL parse and is honored by every HikariCP connection in the pool.
        // Typical cloud values: "require" (Neon, Supabase free tier),
        // "verify-full" (RDS with ACM cert verification).
        if (config.dbSslMode() != null && !config.dbSslMode().isBlank()) {
            hc.addDataSourceProperty("sslmode", config.dbSslMode());
        }

        // Pool sizing — validated by Phase 10.1 (500 virtual threads / 10 connections, 420ms total)
        // and Phase 10.2 (heavy aggregate fetch ~7ms). For a single-user desktop client these are
        // the sweet spot: large enough to absorb UI bursts, small enough to never starve the DB.
        // Ops can override via DB_MAX_POOL_SIZE / DB_CONNECTION_TIMEOUT environment variables
        // when the same artifact is repurposed for multi-tenant or server deployments.
        hc.setMaximumPoolSize(config.maxPoolSize());
        hc.setMinimumIdle(2);
        hc.setConnectionTimeout(config.connectionTimeout());
        hc.setIdleTimeout(600_000);    // 10 min — keeps connections warm for typical sessions
        hc.setMaxLifetime(1_800_000);  // 30 min — recycles to dodge router/NAT idle timeouts

        // Connection-leak detection: warn if a connection is held longer than 60s.
        // HikariCP's leak warning includes only class/method names from the call site
        // (no credentials, no SQL parameters, no row data) — safe for production logs.
        hc.setLeakDetectionThreshold(60_000);

        HikariDataSource ds = new HikariDataSource(hc);
        // Startup line: URL has any embedded creds stripped; user is partially masked;
        // password is never echoed in any form (not even by length).
        log.info("Datasource initialized: url={} user={} sslMode={} pool=[max={} timeout={}ms] leakDetect=60s",
                MaskingUtils.maskJdbcUrl(config.dbUrl()),
                MaskingUtils.mask(config.dbUser()),
                config.dbSslMode() == null ? "default" : config.dbSslMode(),
                config.maxPoolSize(),
                config.connectionTimeout());
        return ds;
    }
}
