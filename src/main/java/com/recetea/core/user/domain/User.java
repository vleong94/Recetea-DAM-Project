package com.recetea.core.user.domain;

import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;

public class User {

    private UserId id;
    private final Username username;
    private final Email email;
    private final PasswordHash passwordHash;

    public User(Username username, Email email, PasswordHash passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void setId(UserId id) { this.id = id; }

    public UserId getId() { return id; }
    public String getUsername() { return username.value(); }
    public String getEmail() { return email.value(); }
    public String getPasswordHash() { return passwordHash.value(); }

    /** passwordHash is intentionally excluded to prevent accidental logging of credential data. */
    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username.value() + ", email=" + email + "]";
    }
}
