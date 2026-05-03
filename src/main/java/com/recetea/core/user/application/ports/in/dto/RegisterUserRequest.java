package com.recetea.core.user.application.ports.in.dto;

import com.recetea.core.shared.domain.ValidationResult;
import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.Username;

/**
 * Inbound command DTO for the registration flow. Centralises all input validation
 * via {@link #validate()} so UI controllers can stay free of duplicated guard
 * blocks: the controller calls {@code validate().getOrThrow(InvalidUserDataException::new)},
 * iterates {@code exception.getErrors()}, and resolves each entry through {@code I18n.get(...)}.
 *
 * <p>Error entries are <strong>i18n keys</strong>, never user-facing strings — that decouples
 * the application layer from any presentation concern (locale, message phrasing, escaping)
 * and lets the controller render multi-line errors and field-level highlighting with a
 * single dispatch step.
 *
 * <p><b>ES — </b>DTO de comando de entrada para el flujo de registro.
 * Centraliza toda la validación de entrada vía {@link #validate()} para
 * que los controladores de UI queden libres de bloques de guard
 * duplicados: el controlador llama
 * {@code validate().getOrThrow(InvalidUserDataException::new)}, itera
 * sobre {@code exception.getErrors()}, y resuelve cada entrada con
 * {@code I18n.get(...)}.
 *
 * <p>Las entradas de error son <strong>claves i18n</strong>, nunca
 * cadenas mostradas al usuario — eso desacopla la capa de aplicación de
 * cualquier preocupación de presentación (locale, redacción del mensaje,
 * escapado) y permite al controlador renderizar errores multi-línea y
 * resaltado por campo en un único paso de despacho.
 */
public record RegisterUserRequest(String username, String email, String password) {

    /**
     * Project-wide minimum password length. Kept here (and not in a VO) because passwords
     * never live as a domain-layer value object — they only flow through this DTO on the
     * way to BCrypt encoding. Mirrored verbatim by the matching i18n message string.
     *
     * <p><b>ES — </b>Longitud mínima de contraseña a nivel de proyecto. Se
     * mantiene aquí (y no en un VO) porque las contraseñas nunca viven
     * como value object del dominio — sólo pasan por este DTO de camino
     * al cifrado BCrypt. Reflejada literalmente en la cadena i18n
     * correspondiente.
     */
    public static final int MIN_PASSWORD_LENGTH = 8;

    public ValidationResult<Void> validate() {
        boolean usernameBlank = username == null || username.isBlank();
        boolean emailBlank    = email    == null || email.isBlank();
        boolean passwordBlank = password == null || password.isBlank();

        return ValidationResult.combine(
            ValidationResult.check(!usernameBlank,
                "register.error.username.required"),
            ValidationResult.check(usernameBlank || username.trim().length() >= Username.MIN_LENGTH,
                "register.error.username.minLength"),
            ValidationResult.check(!emailBlank,
                "register.error.email.required"),
            ValidationResult.check(emailBlank || Email.EMAIL_PATTERN.matcher(email.trim()).matches(),
                "register.error.email.format"),
            ValidationResult.check(!passwordBlank,
                "register.error.password.required"),
            ValidationResult.check(passwordBlank || password.length() >= MIN_PASSWORD_LENGTH,
                "register.error.password.minLength")
        );
    }
}
