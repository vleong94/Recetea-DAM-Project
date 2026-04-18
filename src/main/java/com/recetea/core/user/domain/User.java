package com.recetea.core.user.domain;

public class User {

    private UserId id;
    private final String username;
    private final String email;
    private final String passwordHash;

    public User(String username, String email, String passwordHash) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("El email es obligatorio.");
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("El hash de contraseña es obligatorio.");
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void setId(UserId id) { this.id = id; }

    public UserId getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}
