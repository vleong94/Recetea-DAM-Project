package com.recetea.core.user.domain;

import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;

public class User {

    private UserId id;
    private final Username username;
    private final Email email;
    private final PasswordHash passwordHash;

    public User(String username, String email, String passwordHash) {
        this.username = new Username(username);
        this.email = new Email(email);
        this.passwordHash = new PasswordHash(passwordHash);
    }

    public void setId(UserId id) { this.id = id; }

    public UserId getId() { return id; }
    public String getUsername() { return username.value(); }
    public String getEmail() { return email.value(); }
    public String getPasswordHash() { return passwordHash.value(); }
}
