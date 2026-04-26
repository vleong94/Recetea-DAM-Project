package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Unit;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcUnitRepositoryTest extends BaseRepositoryTest {

    private JdbcUnitRepository repository;
    private JdbcTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new JdbcTransactionManager(dataSource);
        repository = new JdbcUnitRepository(transactionManager);
        seedDatabase();
    }

    private void seedDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO unit_measures (name, abbreviation) VALUES ('Kilogramos', 'kg'), ('Litros', 'l')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findAll_returnsMappedUnitsFromDatabase() {
        List<Unit> units = repository.findAll();

        assertEquals(2, units.size());
        assertTrue(units.stream().anyMatch(u -> u.getAbbreviation().equals("kg")));
    }
}