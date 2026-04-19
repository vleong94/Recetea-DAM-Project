package com.recetea.infrastructure.security;

import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher implements IPasswordEncoder {

    private static final int SALT_ROUNDS = 12;

    public String hash(String plainPassword) {
        return encode(plainPassword);
    }

    public boolean verify(String plainPassword, String hashedPassword) {
        return matches(plainPassword, hashedPassword);
    }

    @Override
    public String encode(String plainText) {
        if (plainText == null || plainText.isEmpty())
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        return BCrypt.hashpw(plainText, BCrypt.gensalt(SALT_ROUNDS));
    }

    @Override
    public boolean matches(String plainText, String encodedText) {
        if (plainText == null || encodedText == null) return false;
        return BCrypt.checkpw(plainText, encodedText);
    }
}
