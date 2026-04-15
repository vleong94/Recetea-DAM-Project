package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de tests de integración para el Outbound Adapter JdbcRecipeRepository.
 * Valida la persistencia atómica, el aislamiento transaccional y la hidratación
 * completa del Aggregate Root desde la infraestructura de datos.
 */
class JdbcRecipeRepositoryTest extends BaseRepositoryTest {

    private JdbcRecipeRepository repository;

    @BeforeEach
    void setUp() {
        // Inicializa el adaptador de infraestructura acoplándolo al DataSource de pruebas.
        repository = new JdbcRecipeRepository(dataSource);
        seedMasterData();
    }

    /**
     * Satisface las restricciones de integridad referencial del esquema físico.
     * Inyecta registros maestros mediante JDBC puro para asegurar que el entorno
     * sea predecible y cumpla con las dependencias requeridas por las Foreign Keys.
     */
    private void seedMasterData() {
        try (Connection conn = dataSource.getConnection()) {
            execute(conn, "INSERT INTO users (id_user, username, email, password_hash) VALUES (1, 'tester', 'test@recetea.com', 'hash')");
            execute(conn, "INSERT INTO categories (id_category, name) VALUES (1, 'Categoría Test')");
            execute(conn, "INSERT INTO difficulties (id_difficulty, level_name) VALUES (1, 'Fácil')");
            execute(conn, "INSERT INTO ingredient_categories (id_ing_category, name) VALUES (1, 'Cat Ingrediente')");
            execute(conn, "INSERT INTO ingredients (id_ingredient, ing_category_id, name) VALUES (1, 1, 'Ingrediente Test')");
            execute(conn, "INSERT INTO unit_measures (id_unit, name, abbreviation) VALUES (1, 'Gramos', 'g')");
        } catch (SQLException e) {
            throw new RuntimeException("Fallo crítico de I/O al sembrar dependencias de Master Data.", e);
        }
    }

    private void execute(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    @Test
    void save_PersisteRecetaYSusIngredientesDeFormaAtomica() {
        // Instancia el Aggregate Root con su identidad de autor encapsulada en un Value Object.
        Recipe recipe = new Recipe(new Recipe.AuthorId(1), 1, 1, "Tortilla de Patatas", "Descripción técnica", 30, 4);
        recipe.addIngredient(new RecipeIngredient(1, 1, new BigDecimal("500.00")));

        // Ejecuta la inserción transaccional.
        repository.save(recipe);

        // Verifica que la identidad física sea asignada e inyectada correctamente en la entidad.
        assertTrue(recipe.getId() > 0, "El repositorio debe inyectar el ID generado tras la inserción.");
        Optional<Recipe> persisted = repository.findById(recipe.getId());
        assertTrue(persisted.isPresent());
        assertEquals(1, persisted.get().getIngredients().size());
    }

    @Test
    void findById_RealizaDeepLoadHidratandoNombresDeDependencias() {
        // Establece una entidad preexistente para la validación de lectura.
        Recipe recipe = new Recipe(new Recipe.AuthorId(1), 1, 1, "Receta Hidratada", "Desc", 20, 2);
        recipe.addIngredient(new RecipeIngredient(1, 1, new BigDecimal("200.00")));
        repository.save(recipe);

        // Recupera la entidad mediante una consulta que incorpora Joins relacionales.
        Optional<Recipe> found = repository.findById(recipe.getId());

        // Confirma que el Data Mapper traduzca los alias de la tabla a las propiedades inmutables del dominio.
        assertTrue(found.isPresent());
        RecipeIngredient hydratedIngredient = found.get().getIngredients().get(0);
        assertEquals("Ingrediente Test", hydratedIngredient.getIngredientName());
        assertEquals("g", hydratedIngredient.getUnitAbbreviation());
    }

    @Test
    void update_SincronizaCambiosMedianteEstrategiaWipeAndReplace() {
        // Posiciona el registro base en la persistencia.
        Recipe recipe = new Recipe(new Recipe.AuthorId(1), 1, 1, "Estado Original", "Desc", 10, 1);
        recipe.addIngredient(new RecipeIngredient(1, 1, new BigDecimal("10.00")));
        repository.save(recipe);

        // Transiciona el estado en memoria, mutando la colección y la cabecera.
        recipe.setTitle("Estado Actualizado");
        recipe.clearIngredients();
        recipe.addIngredient(new RecipeIngredient(1, 1, new BigDecimal("99.99")));

        // Ejecuta el reemplazo atómico en la base de datos.
        repository.update(recipe);

        // Recupera el estado fresco para garantizar la ausencia de datos residuales.
        Recipe updated = repository.findById(recipe.getId()).orElseThrow();
        assertEquals("Estado Actualizado", updated.getTitle());
        assertEquals(1, updated.getIngredients().size(), "La colección debe reflejar exclusivamente el último estado.");
        assertEquals(0, new BigDecimal("99.99").compareTo(updated.getIngredients().get(0).getQuantity()));
    }

    @Test
    void delete_EliminaRecetaYSusIngredientesTransaccionalmente() {
        // Construye y persiste la entidad objetivo.
        Recipe recipe = new Recipe(new Recipe.AuthorId(1), 1, 1, "Entidad a purgar", "Desc", 5, 1);
        recipe.addIngredient(new RecipeIngredient(1, 1, new BigDecimal("1.00")));
        repository.save(recipe);

        // Emite el comando de borrado físico.
        repository.delete(recipe.getId());

        // Asegura que las restricciones transaccionales purguen el registro maestro y sus dependencias.
        Optional<Recipe> deleted = repository.findById(recipe.getId());
        assertTrue(deleted.isEmpty(), "La receta y sus dependencias deben ser eliminadas completamente.");
    }
}