package com.recetea.core.user.domain;

import com.recetea.core.shared.domain.DomainException;
import com.recetea.core.shared.domain.ErrorCode;

/**
 * Thrown when {@code RegisterUserUseCase} detects a username or email
 * that already exists in the {@code users} table — the targeted message
 * tells the form which field collided.
 *
 * <p>Maps to {@link ErrorCode#CONFLICT}. {@code RegisterController}
 * intercepts this exception type directly to drive the inline danger-state
 * UX, bypassing the global handler — the message is meant for the
 * specific field, not a generic dialog.
 *
 * <p><b>ES — </b>Se lanza cuando {@code RegisterUserUseCase} detecta un
 * nombre de usuario o correo que ya existe en la tabla {@code users} — el
 * mensaje específico le dice al formulario qué campo colisionó.
 *
 * <p>Mapea a {@link ErrorCode#CONFLICT}. {@code RegisterController}
 * intercepta este tipo de excepción directamente para activar la UX
 * inline de estado de peligro, saltándose el handler global — el mensaje
 * está pensado para el campo concreto, no para un diálogo genérico.
 */
public class DuplicateIdentityException extends DomainException {

    public DuplicateIdentityException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
