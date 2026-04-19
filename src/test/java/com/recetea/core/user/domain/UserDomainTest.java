package com.recetea.core.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDomainTest {

    private static final String VALID_HASH = "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    @DisplayName("Debe crear un usuario válido con todos sus campos")
    void shouldCreateValidUser() {
        User user = new User("victor", "victor@example.com", VALID_HASH);
        user.setId(new UserId(1));

        assertEquals(1, user.getId().value());
        assertEquals("victor", user.getUsername());
        assertEquals("victor@example.com", user.getEmail());
        assertEquals(VALID_HASH, user.getPasswordHash());
    }

    @Test
    @DisplayName("Debe rechazar UserId con valor no positivo")
    void shouldRejectNonPositiveUserId() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(0));
        assertThrows(IllegalArgumentException.class, () -> new UserId(-1));
    }

    @Test
    @DisplayName("Debe rechazar User con username nulo o vacío")
    void shouldRejectBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> new User(null, "email@example.com", VALID_HASH));
        assertThrows(IllegalArgumentException.class, () -> new User("  ", "email@example.com", VALID_HASH));
    }

    @Test
    @DisplayName("Debe rechazar User con username de menos de 3 caracteres")
    void shouldRejectUsernameShorterThanThreeChars() {
        assertThrows(IllegalArgumentException.class, () -> new User("ab", "email@example.com", VALID_HASH));
        assertThrows(IllegalArgumentException.class, () -> new User("x", "email@example.com", VALID_HASH));
    }

    @Test
    @DisplayName("Debe rechazar User con email nulo o vacío")
    void shouldRejectBlankEmail() {
        assertThrows(IllegalArgumentException.class, () -> new User("victor", null, VALID_HASH));
        assertThrows(IllegalArgumentException.class, () -> new User("victor", "", VALID_HASH));
    }

    @Test
    @DisplayName("Debe rechazar User con email sin formato válido")
    void shouldRejectInvalidEmailFormat() {
        assertThrows(IllegalArgumentException.class, () -> new User("victor", "not-an-email", VALID_HASH));
        assertThrows(IllegalArgumentException.class, () -> new User("victor", "missing@tld", VALID_HASH));
        assertThrows(IllegalArgumentException.class, () -> new User("victor", "@nodomain.com", VALID_HASH));
    }

    @Test
    @DisplayName("Debe rechazar User con passwordHash nulo o vacío")
    void shouldRejectBlankPasswordHash() {
        assertThrows(IllegalArgumentException.class, () -> new User("victor", "email@example.com", null));
        assertThrows(IllegalArgumentException.class, () -> new User("victor", "email@example.com", "  "));
    }

    @Test
    @DisplayName("Debe rechazar User con passwordHash que no sea un hash BCrypt")
    void shouldRejectNonBcryptPasswordHash() {
        assertThrows(IllegalArgumentException.class,
                () -> new User("victor", "email@example.com", "plaintext_password"));
        assertThrows(IllegalArgumentException.class,
                () -> new User("victor", "email@example.com", "md5hashvalue"));
    }
}
