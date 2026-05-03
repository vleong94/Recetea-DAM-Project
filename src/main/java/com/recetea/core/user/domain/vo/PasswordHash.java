package com.recetea.core.user.domain.vo;

/**
 * Wrapper around a BCrypt-hashed password string. Two purposes: enforce the
 * {@code $2…} prefix invariant (a plain-text password slipping into this slot
 * is a serious bug — surface it loudly), and override {@link #toString()}
 * so the hash never leaks into log output, debugger string representations,
 * or default record-toString chains.
 *
 * <p>Compact-constructor invariants: not null, not blank, and starts with
 * {@code $2} (the BCrypt format prefix used by every {@code $2a$ / $2b$ / $2y$}
 * variant). The full regex shape ({@code ^\$2[ayb]\$\d{2}\$.{53}$}) is
 * additionally scrubbed from log output by {@code SensitiveDataMaskingConverter}
 * — defense in depth in case any code path ever bypasses this wrapper.
 *
 * <p><b>ES — </b>Envoltorio sobre una contraseña ya hasheada con BCrypt. Dos
 * propósitos: aplicar el invariante del prefijo {@code $2…} (una contraseña
 * en texto plano que se cuele en este slot es un bug serio — hay que
 * exponerlo de forma ruidosa), y sobrescribir {@link #toString()} para que el
 * hash no se filtre nunca en la salida de logs, en las representaciones del
 * depurador o en las cadenas {@code toString} por defecto de los records.
 *
 * <p>Invariantes del constructor compacto: no nulo, no en blanco, y empieza
 * por {@code $2} (el prefijo del formato BCrypt usado por todas las variantes
 * {@code $2a$ / $2b$ / $2y$}). La forma completa del regex
 * ({@code ^\$2[ayb]\$\d{2}\$.{53}$}) se filtra adicionalmente en la salida de
 * logs mediante {@code SensitiveDataMaskingConverter} — defensa en
 * profundidad por si alguna ruta de código se salta este wrapper.
 */
public record PasswordHash(String value) {
    public PasswordHash {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Password hash is required.");
        if (!value.startsWith("$2"))
            throw new IllegalArgumentException("Password hash must be a valid BCrypt hash.");
    }

    /** Prevents the hash from appearing in logs, debugger output, or default {@code toString} chains. */
    @Override
    public String toString() {
        return "PasswordHash[value=PROTECTED]";
    }
}
