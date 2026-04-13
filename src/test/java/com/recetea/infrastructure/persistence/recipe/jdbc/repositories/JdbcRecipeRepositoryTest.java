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
 * Verifica la persistencia atómica y la hidratación del Aggregate Root completo,
 * garantizando el aislamiento del entorno mediante la clase base de infraestructura.
 */
class JdbcRecipeRepositoryTest extends BaseRepositoryTest {

    private JdbcRecipeRepository repository;

    @BeforeEach
    void setUp() {
        // Inicialización del adaptador de infraestructura y preparación de dependencias maestras.
        repository = new JdbcRecipeRepository(dataSource);
        seedMasterData();
    }

    /**
     * Satisface las restricciones de integridad referencial del esquema físico.
     * Inyecta registros en las entidades maestras siguiendo el orden jerárquico
     * obligatorio para permitir la persistencia de una Recipe válida.
     */
    private void seedMasterData() {
        try (Connection conn = dataSource.getConnection()) {
            // Entidades de autorización y clasificación base
            execute(conn, "INSERT INTO users (id_user, username, email, password_hash) VALUES (1, 'tester', 'test@recetea.com', 'hash')");
            execute(conn, "INSERT INTO categories (id_category, name) VALUES (1, 'Categoría Test')");
            execute(conn, "INSERT INTO difficulties (id_difficulty, level_name) VALUES (1, 'Fácil')");

            // Entidades maestras de componentes de receta
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
    void save_PersisteRecetaYSusIngredientesDeFormaAtómica() {
        // Inicialización del Aggregate Root en memoria.
        Recipe recipe = new Recipe(1, 1, 1, "Tortilla de Patatas", "Descripción técnica", 30, 4);
        recipe.addIngredient(new RecipeIngredient(1, 1, new BigDecimal("500.00")));

        // Ejecución de la transacción de guardado.
        repository.save(recipe);

        // Verificación de la inyección de identidad y persistencia física.
        assertTrue(recipe.getId() > 0, "El repositorio debe inyectar el ID generado tras la inserción.");
        Optional<Recipe> persisted = repository.findById(recipe.getId());
        assertTrue(persisted.isPresent());
        assertEquals(1, persisted.get().getIngredients().size());
    }

    @Test
    void findById_RealizaDeepLoadHidratandoNombresDeDependencias() {
        // Establecimiento de un estado inicial persistido para control.
        Recipe recipe = new Recipe(1, 1, 1, "Receta Hidratada", "Desc", 20, 2);
        recipe.addIngredient(new RecipeIngredient(1, 1, new BigDecimal("200.00")));
        repository.save(recipe);

        // Ejecución de lectura con hidratación profunda (Joins).
        Optional<Recipe> found = repository.findById(recipe.getId());

        // Validación de la carga de metadatos descriptivos desde tablas maestras.
        assertTrue(found.isPresent());
        RecipeIngredient hydratedIngredient = found.get().getIngredients().get(0);
        assertEquals("Ingrediente Test", hydratedIngredient.getIngredientName());
        assertEquals("g", hydratedIngredient.getUnitAbbreviation());
    }

    @Test
    void update_SincronizaCambiosMedianteEstrategiaWipeAndReplace() {
        // Configuración del estado original en el Data Store.
        Recipe recipe = new Recipe(1, 1, 1, "Estado Original", "Desc", 10, 1);
        recipe.addIngredient(new RecipeIngredient(1, 1, new BigDecimal("10.00")));
        repository.save(recipe);

        // Mutación del estado mediante métodos autorizados del dominio.
        // Se limpia la colección interna y se inyecta el nuevo estado para validar la sincronización.
        recipe.setTitle("Estado Actualizado");
        recipe.clearIngredients();
        recipe.addIngredient(new RecipeIngredient(1, 1, new BigDecimal("99.99")));

        // Ejecución de la actualización transaccional.
        repository.update(recipe);

        // Verificación de la integridad del nuevo estado y ausencia de duplicados.
        Recipe updated = repository.findById(recipe.getId()).orElseThrow();
        assertEquals("Estado Actualizado", updated.getTitle());
        assertEquals(1, updated.getIngredients().size(), "La estrategia Wipe & Replace debe garantizar una colección limpia.");
        assertEquals(0, new BigDecimal("99.99").compareTo(updated.getIngredients().get(0).getQuantity()));
    }

    @Test
    void delete_EliminaRecetaYSusIngredientesTransaccionalmente() {
        // Preparación del registro a eliminar.
        Recipe recipe = new Recipe(1, 1, 1, "Entidad a purgar", "Desc", 5, 1);
        recipe.addIngredient(new RecipeIngredient(1, 1, new BigDecimal("1.00")));
        repository.save(recipe);

        // Ejecución del borrado físico.
        repository.delete(recipe.getId());

        // Confirmación de la purga total en la base de datos.
        Optional<Recipe> deleted = repository.findById(recipe.getId());
        assertTrue(deleted.isEmpty(), "La receta y sus dependencias deben ser eliminadas completamente.");
    }
}