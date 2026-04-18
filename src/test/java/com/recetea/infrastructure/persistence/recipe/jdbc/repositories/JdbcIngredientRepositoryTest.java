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
        repository = new JdbcIngredientRepository(transactionManager);
        seedDatabase();
    }

    private void seedDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            String sqlCat = "INSERT INTO ingredient_categories (id_ing_category, name) VALUES (1, 'Test Cat')";
            String sqlIng = "INSERT INTO ingredients (name, ing_category_id) VALUES ('Sal', 1), ('Pimienta', 1)";

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
    void findAll_DebeRetornarTodosLosIngredientesOrdenados() {
        List<Ingredient> ingredients = repository.findAll();

        assertNotNull(ingredients);
        assertEquals(2, ingredients.size());
        assertEquals("Pimienta", ingredients.get(0).getName()); // Orden lexicográfico
        assertEquals("Sal", ingredients.get(1).getName());
    }
}