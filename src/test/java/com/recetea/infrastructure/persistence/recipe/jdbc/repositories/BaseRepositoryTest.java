package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.infrastructure.persistence.recipe.jdbc.config.DatabaseConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Orquestador base para los Integration Tests de persistencia.
 * Garantiza que cada ejecución parta de un estado de base de datos purgado (Pure State),
 * previniendo fugas de estado entre tests y asegurando la idempotencia transaccional.
 */
public abstract class BaseRepositoryTest {

    protected static DataSource dataSource;

    /**
     * Fuerza el contexto de ejecución al entorno de pruebas aislando la configuración.
     * Configura el Environment Variable para que el Singleton de infraestructura
     * conecte exclusivamente contra el Data Source de sacrificio.
     */
    @BeforeAll
    static void setupEnvironment() {
        System.setProperty("env", "test");
        dataSource = DatabaseConfig.getDataSource();
    }

    /**
     * Ejecuta una purga profunda del esquema completo antes de cada evaluación.
     * Trunca todas las entidades del dominio y Master Data, reiniciando
     * las secuencias de identidad para asegurar un entorno de ejecución predecible.
     */
    @BeforeEach
    void cleanDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Ejecución del Cleanup masivo.
            // Se listan todas las tablas maestras explícitamente para garantizar
            // el RESTART IDENTITY, mientras que CASCADE resuelve las Foreign Keys.
            stmt.execute("TRUNCATE TABLE " +
                    "ratings, favorites, recipe_tags, recipe_media, steps, recipe_ingredients, " +
                    "recipes, ingredients, unit_measures, ingredient_categories, " +
                    "difficulties, categories, tags, users " +
                    "RESTART IDENTITY CASCADE");

        } catch (SQLException e) {
            throw new RuntimeException("Fallo crítico de infraestructura: Imposible aplicar el Wipe & Reset en el entorno PostgreSQL.", e);
        }
    }
}