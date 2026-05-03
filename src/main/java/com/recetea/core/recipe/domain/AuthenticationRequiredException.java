package com.recetea.core.recipe.domain;

import com.recetea.core.shared.domain.DomainException;
import com.recetea.core.shared.domain.ErrorCode;

/**
 * Thrown when a use case that requires a logged-in user is invoked with no
 * active session. {@code GlobalExceptionHandler} maps the
 * {@link ErrorCode#AUTHENTICATION_REQUIRED} code to a localised dialog;
 * the controller that triggered the call is responsible for routing back
 * to the login screen if appropriate.
 *
 * <p>Located under the {@code recipe} domain package for historical reasons
 * (most early use cases needing it lived in recipe); it is shared across
 * recipe / social / interop flows.
 *
 * <p><b>ES — </b>Se lanza cuando se invoca un caso de uso que requiere un
 * usuario logueado sin sesión activa. {@code GlobalExceptionHandler}
 * mapea el código {@link ErrorCode#AUTHENTICATION_REQUIRED} a un diálogo
 * localizado; el controlador que disparó la llamada es responsable de
 * redirigir de vuelta a la pantalla de login si procede.
 *
 * <p>Ubicado bajo el paquete de dominio {@code recipe} por razones
 * históricas (la mayoría de los primeros casos de uso que la necesitaban
 * vivían en recipe); se comparte entre los flujos de recipe / social /
 * interop.
 */
public class AuthenticationRequiredException extends DomainException {

    public AuthenticationRequiredException() {
        super(ErrorCode.AUTHENTICATION_REQUIRED, "Authentication is required to perform this operation.");
    }
}
