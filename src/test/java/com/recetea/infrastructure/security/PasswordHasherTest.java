package com.recetea.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    @DisplayName("Un hash verificado con la contraseña original debe retornar true")
    void hash_ShouldProduceVerifiableHash() {
        String hash = hasher.hash("miContraseña123");
        assertTrue(hasher.verify("miContraseña123", hash));
    }

    @Test
    @DisplayName("La verificación debe fallar con una contraseña incorrecta")
    void verify_ShouldReturnFalse_WhenPasswordDoesNotMatch() {
        String hash = hasher.hash("contraseñaCorrecta");
        assertFalse(hasher.verify("contraseñaIncorrecta", hash));
    }

    @Test
    @DisplayName("Dos hashes de la misma contraseña deben ser distintos por el salt aleatorio")
    void hash_ShouldProduceDifferentHashesForSamePassword() {
        String hash1 = hasher.hash("mismaContraseña");
        String hash2 = hasher.hash("mismaContraseña");
        assertNotEquals(hash1, hash2, "BCrypt debe generar salts distintos en cada llamada");
    }

    @Test
    @DisplayName("hash debe lanzar excepción si la contraseña es nula o vacía")
    void hash_ShouldThrow_WhenPasswordIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(""));
    }

    @Test
    @DisplayName("verify debe retornar false si alguno de los argumentos es nulo")
    void verify_ShouldReturnFalse_WhenArgumentsAreNull() {
        assertFalse(hasher.verify(null, "$2a$12$validhash"));
        assertFalse(hasher.verify("password", null));
    }
}
