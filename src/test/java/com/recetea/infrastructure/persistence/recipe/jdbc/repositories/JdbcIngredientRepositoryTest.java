package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Ingredient;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcIngredientRepositoryTest extends BaseRepositoryTest {

    private JdbcIngredientRepository repository;
    private JdbcTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new JdbcTransactionManager(dataSource);
        repository = new JdbcIngredientRepository(transactionManager, metricsPort);
        seedDatabase();
    }

    private void seedDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            String sqlCat = "INSERT INTO ingredient_categories (ingredient_category_id, name) VALUES (1, 'Test Cat')";
            String sqlIng = "INSERT INTO ingredients (name, ingredient_category_id) VALUES ('Salt', 1), ('Pepper', 1)";

            try (PreparedStatement ps1 = conn.prepareStatement(sqlCat);
                 PreparedStatement ps2 = conn.prepareStatement(sqlIng)) {
                ps1.executeUpdate();
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findAll_ShouldReturnAllIngredientsOrdered() {
        List<Ingredient> ingredients = repository.findAll();

        assertNotNull(ingredients);
        assertEquals(2, ingredients.size());
        assertEquals("Pepper", ingredients.get(0).name()); // Lexicographic order
        assertEquals("Salt", ingredients.get(1).name());
    }
}