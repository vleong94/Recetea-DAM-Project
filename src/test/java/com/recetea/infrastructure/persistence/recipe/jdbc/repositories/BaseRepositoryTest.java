package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.infrastructure.persistence.recipe.jdbc.config.DatabaseConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Clase base para Integration Tests de persistencia.
 * Garantiza un Pure State en la base de datos de sacrificio (test) mediante
 * operaciones de truncado masivo antes de cada método de prueba, asegurando
 * que los resultados sean predecibles y no exista filtración de estado.
 */
public abstract class BaseRepositoryTest {

    protected static DataSource dataSource;

    @BeforeAll
    static void setupEnvironment() {
        // Forza el uso de application-test.properties
        System.setProperty("env", "test");
        dataSource = DatabaseConfig.getDataSource();
    }

    @BeforeEach
    void cleanDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Purgado atómico de todas las tablas respetando la jerarquía de Foreign Keys
            stmt.execute("TRUNCATE TABLE " +
                    "ratings, favorites, recipe_tags, recipe_media, steps, recipe_ingredients, " +
                    "recipes, ingredients, unit_measures, ingredient_categories, " +
                    "difficulties, categories, tags, users " +
                    "RESTART IDENTITY CASCADE");

        } catch (SQLException e) {
            throw new RuntimeException("Fallo crítico: No se pudo resetear el entorno de pruebas.", e);
        }
    }
}