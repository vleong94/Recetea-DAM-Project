package com.recetea.core.user.domain;

import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;

import java.util.Objects;

/**
 * Account identity — one row in {@code users}. Two constructors mirror the
 * record / VO pattern across the codebase: the create-path (no id, used
 * before persistence) and the canonical 4-arg form (used by row mappers
 * after the id is known).
 *
 * <p>{@link #toString()} deliberately excludes {@code passwordHash} so
 * accidental log-line interpolation of the record never leaks the BCrypt
 * token. The {@code SensitiveDataMaskingConverter} on Logback is the
 * second line of defense — both layers must hold for the credential
 * scrubbing guarantee.
 *
 * <p><b>ES — </b>Identidad de cuenta — una fila en {@code users}. Dos
 * constructores reflejan el patrón record / VO del resto del código: el
 * camino de creación (sin id, usado antes de persistir) y la forma
 * canónica de 4 args (usada por los row mappers cuando el id ya se conoce).
 *
 * <p>{@link #toString()} excluye deliberadamente {@code passwordHash} para
 * que la interpolación accidental del record en una línea de log nunca
 * filtre el token BCrypt. El {@code SensitiveDataMaskingConverter} de
 * Logback es la segunda línea de defensa — ambas capas deben mantenerse
 * para garantizar el filtrado de credenciales.
 */
public record User(UserId id, Username username, Email email, PasswordHash passwordHash) {

    public User {
        // id may be null — represents the create-path, before persistence assigns one.
        Objects.requireNonNull(username,     "username is required.");
        Objects.requireNonNull(email,        "email is required.");
        Objects.requireNonNull(passwordHash, "passwordHash is required.");
    }

    /** Create-path constructor — used when registering a new user (id is unknown until persisted). */
    public User(Username username, Email email, PasswordHash passwordHash) {
        this(null, username, email, passwordHash);
    }

    /** passwordHash is intentionally excluded to prevent accidental logging of credential data. */
    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username.value() + ", email=" + email.value() + "]";
    }
}
