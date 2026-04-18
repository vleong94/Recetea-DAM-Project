package com.recetea.infrastructure.persistence.user.jdbc.repositories;

import com.recetea.core.user.domain.User;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.BaseRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JdbcUserRepositoryTest extends BaseRepositoryTest {

    private JdbcUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcUserRepository(new JdbcTransactionManager(dataSource));
    }

    @Test
    @DisplayName("save + findById deben hacer un roundtrip completo")
    void save_AndFindById_ShouldRoundtrip() {
        User user = new User("victor", "victor@example.com", "hashed_pw");
        repository.save(user);

        assertNotNull(user.getId(), "El id debe ser asignado tras el save");

        Optional<User> found = repository.findById(user.getId());
        assertTrue(found.isPresent());
        assertEquals("victor", found.get().getUsername());
        assertEquals("victor@example.com", found.get().getEmail());
        assertEquals("hashed_pw", found.get().getPasswordHash());
        assertEquals(user.getId().value(), found.get().getId().value());
    }

    @Test
    @DisplayName("findByUsername debe resolver un usuario existente")
    void findByUsername_ShouldReturnUser_WhenExists() {
        repository.save(new User("maria", "maria@example.com", "pw123"));

        Optional<User> found = repository.findByUsername("maria");
        assertTrue(found.isPresent());
        assertEquals("maria@example.com", found.get().getEmail());
    }

    @Test
    @DisplayName("findByUsername debe devolver Optional.empty para usuarios inexistentes")
    void findByUsername_ShouldReturnEmpty_WhenNotFound() {
        Optional<User> found = repository.findByUsername("ghost");
        assertTrue(found.isEmpty());
    }
}
