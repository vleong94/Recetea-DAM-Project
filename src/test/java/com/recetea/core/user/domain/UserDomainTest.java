package com.recetea.core.user.domain;

import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDomainTest {

    private static final String VALID_HASH = "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    @DisplayName("Debe crear un usuario válido con todos sus campos")
    void shouldCreateValidUser() {
        User user = new User(
                new Username("victor"),
                new Email("victor@example.com"),
                new PasswordHash(VALID_HASH)
        );
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
    @DisplayName("Username debe rechazar valor nulo o vacío")
    void shouldRejectBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> new Username(null));
        assertThrows(IllegalArgumentException.class, () -> new Username("  "));
    }

    @Test
    @DisplayName("Username debe rechazar menos de 3 caracteres")
    void shouldRejectUsernameShorterThanThreeChars() {
        assertThrows(IllegalArgumentException.class, () -> new Username("ab"));
        assertThrows(IllegalArgumentException.class, () -> new Username("x"));
    }

    @Test
    @DisplayName("Email debe rechazar valor nulo o vacío")
    void shouldRejectBlankEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
        assertThrows(IllegalArgumentException.class, () -> new Email(""));
    }

    @Test
    @DisplayName("Email debe rechazar formatos inválidos")
    void shouldRejectInvalidEmailFormat() {
        assertThrows(IllegalArgumentException.class, () -> new Email("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> new Email("missing@tld"));
        assertThrows(IllegalArgumentException.class, () -> new Email("@nodomain.com"));
    }

    @Test
    @DisplayName("PasswordHash debe rechazar valor nulo o vacío")
    void shouldRejectBlankPasswordHash() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash(null));
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash("  "));
    }

    @Test
    @DisplayName("PasswordHash debe rechazar hashes que no sean BCrypt")
    void shouldRejectNonBcryptPasswordHash() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash("plaintext_password"));
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash("md5hashvalue"));
    }
}
