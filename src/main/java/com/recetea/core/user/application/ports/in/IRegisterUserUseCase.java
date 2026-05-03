package com.recetea.core.user.application.ports.in;

import com.recetea.core.user.application.ports.in.dto.RegisterUserRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;

/**
 * Creates a new user account. Throws {@code DuplicateIdentityException}
 * (extends {@code DomainException}, code {@code CONFLICT}) when the
 * username or email is already taken — the controller maps that to the
 * inline danger-state UX (the global handler is bypassed for the validation
 * branch and exclusively handles infrastructure-level failures).
 *
 * <p>The implementation hashes the password with BCrypt-12 before
 * persisting; the request DTO carries plaintext, the {@code User} aggregate
 * carries only the hash.
 *
 * <p><b>ES — </b>Crea una nueva cuenta de usuario. Lanza
 * {@code DuplicateIdentityException} (hereda de {@code DomainException},
 * código {@code CONFLICT}) cuando el username o email ya están ocupados
 * — el controlador mapea eso a la UX de estado de peligro inline (el
 * handler global se salta en la rama de validación y maneja
 * exclusivamente los fallos de infraestructura).
 *
 * <p>La implementación hashea la contraseña con BCrypt-12 antes de
 * persistir; el DTO de request lleva texto plano, el agregado
 * {@code User} lleva sólo el hash.
 */
public interface IRegisterUserUseCase {

    UserResponse execute(RegisterUserRequest request);
}
