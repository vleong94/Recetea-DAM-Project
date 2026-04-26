package com.recetea.infrastructure.persistence.user.jdbc.repositories;

import com.recetea.core.user.domain.User;
import com.recetea.core.user.domain.UserId;
import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;
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

    private static final String HASH_A = "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B = "$2a$12$bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Test
    @DisplayName("save + findById deben hacer un roundtrip completo")
    void save_AndFindById_ShouldRoundtrip() {
        User user = new User(new Username("victor"), new Email("victor@example.com"), new PasswordHash(HASH_A));
        repository.save(user);

        assertNotNull(user.getId(), "El id debe ser asignado tras el save");

        Optional<User> found = repository.findById(user.getId());
        assertTrue(found.isPresent());
        assertEquals("victor", found.get().getUsername());
        assertEquals("victor@example.com", found.get().getEmail());
        assertEquals(HASH_A, found.get().getPasswordHash());
        assertEquals(user.getId().value(), found.get().getId().value());
    }

    @Test
    @DisplayName("findByUsername debe resolver un usuario existente")
    void findByUsername_ShouldReturnUser_WhenExists() {
        repository.save(new User(new Username("maria"), new Email("maria@example.com"), new PasswordHash(HASH_B)));

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

    @Test
    @DisplayName("findByEmail debe resolver un usuario existente y preservar el hash")
    void findByEmail_ShouldReturnUser_AndPreservePasswordHash() {
        repository.save(new User(new Username("ana"), new Email("ana@example.com"), new PasswordHash(HASH_A)));

        Optional<User> found = repository.findByEmail("ana@example.com");
        assertTrue(found.isPresent());
        assertEquals("ana", found.get().getUsername());
        assertEquals(HASH_A, found.get().getPasswordHash(),
                "El roundtrip debe preservar el hash de contraseña exactamente");
    }

    @Test
    @DisplayName("findByEmail debe devolver Optional.empty para emails inexistentes")
    void findByEmail_ShouldReturnEmpty_WhenNotFound() {
        Optional<User> found = repository.findByEmail("nobody@example.com");
        assertTrue(found.isEmpty());
    }
}
