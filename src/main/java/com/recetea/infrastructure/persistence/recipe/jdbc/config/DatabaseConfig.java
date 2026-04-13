package com.recetea.infrastructure.persistence.recipe.jdbc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton de infraestructura que gestiona la provisión de conexiones a la base de datos.
 * Implementa una estrategia de carga dinámica de propiedades para alternar entre
 * entornos de desarrollo y test.
 * Utiliza HikariCP como Connection Pool para optimizar el ciclo de vida de las conexiones.
 */
public class DatabaseConfig {

    private static volatile DataSource dataSource;

    private DatabaseConfig() {
        // Evita la instanciación para mantener el patrón Singleton.
    }

    /**
     * Provee acceso al DataSource mediante Lazy Initialization y un bloque
     * Thread-Safe con Double-Checked Locking para evitar Race Conditions
     * en entornos concurrentes.
     * * @return DataSource configurado con el Connection Pool activo.
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

    /**
     * Inicializa el Connection Pool leyendo el archivo de propiedades del Classpath.
     * El archivo se determina dinámicamente mediante la variable de entorno 'env'
     * ('application-local.properties' por defecto).
     * Aplica el patrón Fail-Fast interrumpiendo la ejecución inmediatamente si
     * la configuración no existe o es defectuosa.
     * * @return Instancia de HikariDataSource lista para despachar conexiones.
     */
    private static DataSource buildDataSource() {
        Properties properties = new Properties();
        String env = System.getProperty("env", "local");
        String fileName = "application-" + env + ".properties";

        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                throw new IllegalStateException("Fallo en el Classpath: No se encontró el archivo de configuración " + fileName);
            }
            properties.load(input);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(properties.getProperty("db.url"));
            config.setUsername(properties.getProperty("db.user"));
            config.setPassword(properties.getProperty("db.password"));

            // Configuraciones de resiliencia del Connection Pool
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            return new HikariDataSource(config);

        } catch (IOException e) {
            throw new RuntimeException("Fallo crítico de I/O al cargar las propiedades de la base de datos.", e);
        }
    }
}