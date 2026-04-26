package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JdbcCategoryRepositoryTest extends BaseRepositoryTest {

    private JdbcCategoryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcCategoryRepository(new JdbcTransactionManager(dataSource));
        seedDatabase();
    }

    private void seedDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO categories (name) VALUES ('Postres'), ('Sopas')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int firstCategoryId() {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id_category FROM categories ORDER BY id_category LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("No categories seeded");
    }

    @Test
    void findAll_returnsMappedCategoriesFromDatabase() {
        List<Category> categories = repository.findAll();

        assertEquals(2, categories.size());
        assertTrue(categories.stream().anyMatch(c -> c.getName().equals("Postres")));
        assertTrue(categories.stream().anyMatch(c -> c.getName().equals("Sopas")));
    }

    @Test
    void findAll_returnsCachedResultOnSecondCall() {
        List<Category> first = repository.findAll();
        List<Category> second = repository.findAll();

        assertSame(first, second);
    }

    @Test
    void findById_returnsCorrectCategoryFromCache() {
        int id = firstCategoryId();
        Optional<Category> result = repository.findById(new CategoryId(id));

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId().value());
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        Optional<Category> result = repository.findById(new CategoryId(99999));

        assertTrue(result.isEmpty());
    }
}
