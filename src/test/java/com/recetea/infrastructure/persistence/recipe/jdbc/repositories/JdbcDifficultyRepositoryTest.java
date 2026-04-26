package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.vo.DifficultyId;
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

class JdbcDifficultyRepositoryTest extends BaseRepositoryTest {

    private JdbcDifficultyRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcDifficultyRepository(new JdbcTransactionManager(dataSource));
        seedDatabase();
    }

    private void seedDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO difficulties (level_name) VALUES ('Fácil'), ('Difícil')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int firstDifficultyId() {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id_difficulty FROM difficulties ORDER BY id_difficulty LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("No difficulties seeded");
    }

    @Test
    void findAll_returnsMappedDifficultiesFromDatabase() {
        List<Difficulty> difficulties = repository.findAll();

        assertEquals(2, difficulties.size());
        assertTrue(difficulties.stream().anyMatch(d -> d.getName().equals("Fácil")));
        assertTrue(difficulties.stream().anyMatch(d -> d.getName().equals("Difícil")));
    }

    @Test
    void findAll_returnsCachedResultOnSecondCall() {
        List<Difficulty> first = repository.findAll();
        List<Difficulty> second = repository.findAll();

        assertSame(first, second);
    }

    @Test
    void findById_returnsCorrectDifficultyFromCache() {
        int id = firstDifficultyId();
        Optional<Difficulty> result = repository.findById(new DifficultyId(id));

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId().value());
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        Optional<Difficulty> result = repository.findById(new DifficultyId(99999));

        assertTrue(result.isEmpty());
    }
}
