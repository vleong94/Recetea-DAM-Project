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
        repository = new JdbcUserRepository(new JdbcTransactionManager(dataSource), metricsPort);
    }

    private static final String HASH_A = "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B = "$2a$12$bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Test
    @DisplayName("save and findById should complete a full roundtrip")
    void save_AndFindById_ShouldRoundtrip() {
        User user = new User(new Username("victor"), new Email("victor@example.com"), new PasswordHash(HASH_A));
        UserId savedId = repository.save(user);

        assertNotNull(savedId, "ID should be assigned after save");

        Optional<User> found = repository.findById(savedId);
        assertTrue(found.isPresent());
        assertEquals("victor", found.get().username().value());
        assertEquals("victor@example.com", found.get().email().value());
        assertEquals(HASH_A, found.get().passwordHash().value());
        assertEquals(savedId.value(), found.get().id().value());
    }

    @Test
    @DisplayName("findByUsername should return an existing user")
    void findByUsername_ShouldReturnUser_WhenExists() {
        repository.save(new User(new Username("maria"), new Email("maria@example.com"), new PasswordHash(HASH_B)));

        Optional<User> found = repository.findByUsername("maria");
        assertTrue(found.isPresent());
        assertEquals("maria@example.com", found.get().email().value());
    }

    @Test
    @DisplayName("findByUsername should return Optional.empty for non-existent users")
    void findByUsername_ShouldReturnEmpty_WhenNotFound() {
        Optional<User> found = repository.findByUsername("ghost");
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("findByEmail should return an existing user and preserve the password hash")
    void findByEmail_ShouldReturnUser_AndPreservePasswordHash() {
        repository.save(new User(new Username("ana"), new Email("ana@example.com"), new PasswordHash(HASH_A)));

        Optional<User> found = repository.findByEmail("ana@example.com");
        assertTrue(found.isPresent());
        assertEquals("ana", found.get().username().value());
        assertEquals(HASH_A, found.get().passwordHash().value(),
                "Roundtrip should preserve the password hash exactly");
    }

    @Test
    @DisplayName("findByEmail should return Optional.empty for non-existent emails")
    void findByEmail_ShouldReturnEmpty_WhenNotFound() {
        Optional<User> found = repository.findByEmail("nobody@example.com");
        assertTrue(found.isEmpty());
    }
}
