package com.recetea.core.user.application.ports.in.dto;

import com.recetea.core.shared.domain.ValidationResult;

/**
 * Inbound DTO for {@code ILoginUseCase}. Carries the user's identifier
 * (either a username or an email — the use case probes both lookup paths
 * in order) and the plaintext password the user typed.
 *
 * <p>The plaintext is intentional: BCrypt verification needs the cleartext
 * to recompute the hash. The {@code SensitiveDataMaskingConverter} attached
 * to Logback ensures any password-shaped string accidentally reaching a
 * log statement is scrubbed before reaching the appender — defense in depth
 * against developer error.
 *
 * <p><b>ES — </b>DTO de entrada para {@code ILoginUseCase}. Lleva el
 * identificador del usuario (ya sea username o email — el caso de uso
 * prueba ambos caminos de lookup en orden) y la contraseña en texto
 * plano que tecleó el usuario.
 *
 * <p>El texto plano es intencional: la verificación BCrypt necesita el
 * texto plano para recomputar el hash. El
 * {@code SensitiveDataMaskingConverter} acoplado a Logback garantiza que
 * cualquier cadena con forma de contraseña que accidentalmente llegue a
 * un log se filtre antes de alcanzar el appender — defensa en
 * profundidad frente a errores del desarrollador.
 */
public record LoginRequest(String usernameOrEmail, String password) {

    /**
     * Required-fields check; pure structural validation, no DB or auth probe.
     *
     * <p><b>ES — </b>Comprobación de campos obligatorios; validación
     * puramente estructural, sin sondeos a la BD ni autenticación.
     */
    public ValidationResult<Void> validate() {
        return ValidationResult.combine(
            ValidationResult.check(usernameOrEmail != null && !usernameOrEmail.isBlank(),
                "Username or email is required."),
            ValidationResult.check(password != null && !password.isBlank(),
                "Password is required.")
        );
    }
}
