package com.recetea.core.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDomainTest {

    @Test
    @DisplayName("Debe crear un usuario válido con todos sus campos")
    void shouldCreateValidUser() {
        User user = new User("victor", "victor@example.com", "hashed_password");
        user.setId(new UserId(1));

        assertEquals(1, user.getId().value());
        assertEquals("victor", user.getUsername());
        assertEquals("victor@example.com", user.getEmail());
        assertEquals("hashed_password", user.getPasswordHash());
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
        assertThrows(IllegalArgumentException.class, () -> new User(null, "email@example.com", "hash"));
        assertThrows(IllegalArgumentException.class, () -> new User("  ", "email@example.com", "hash"));
    }

    @Test
    @DisplayName("Debe rechazar User con email nulo o vacío")
    void shouldRejectBlankEmail() {
        assertThrows(IllegalArgumentException.class, () -> new User("victor", null, "hash"));
        assertThrows(IllegalArgumentException.class, () -> new User("victor", "", "hash"));
    }

    @Test
    @DisplayName("Debe rechazar User con passwordHash nulo o vacío")
    void shouldRejectBlankPasswordHash() {
        assertThrows(IllegalArgumentException.class, () -> new User("victor", "email@example.com", null));
        assertThrows(IllegalArgumentException.class, () -> new User("victor", "email@example.com", "  "));
    }
}
