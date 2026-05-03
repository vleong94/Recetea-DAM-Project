package com.recetea.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    @DisplayName("hash: Should produce a hash that verifies against the original password")
    void hash_ShouldProduceVerifiableHash() {
        String hash = hasher.hash("myPassword123");
        assertTrue(hasher.verify("myPassword123", hash));
    }

    @Test
    @DisplayName("verify: Should return false when the password does not match")
    void verify_ShouldReturnFalse_WhenPasswordDoesNotMatch() {
        String hash = hasher.hash("correctPassword");
        assertFalse(hasher.verify("wrongPassword", hash));
    }

    @Test
    @DisplayName("hash: Should produce different hashes for the same password due to random salt")
    void hash_ShouldProduceDifferentHashesForSamePassword() {
        String hash1 = hasher.hash("samePassword");
        String hash2 = hasher.hash("samePassword");
        assertNotEquals(hash1, hash2, "BCrypt must generate different salts on each call");
    }

    @Test
    @DisplayName("hash: Should throw an exception when the password is null or blank")
    void hash_ShouldThrow_WhenPasswordIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(""));
    }

    @Test
    @DisplayName("verify: Should return false when any argument is null")
    void verify_ShouldReturnFalse_WhenArgumentsAreNull() {
        assertFalse(hasher.verify(null, "$2a$12$validhash"));
        assertFalse(hasher.verify("password", null));
    }
}
