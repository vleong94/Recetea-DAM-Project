package com.recetea.core.user.application.ports.in;

import com.recetea.core.user.application.ports.in.dto.LoginRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;

import java.util.Optional;

/**
 * Authenticates a user by username-or-email + password.
 *
 * <p><b>Empty-Optional contract for invalid credentials.</b> An incorrect
 * username/email or password resolves to {@link Optional#empty()} —
 * <em>not</em> a thrown exception. This is deliberate: an authentication-
 * failed exception would leak through the global handler as a stack trace
 * and trigger the operator-facing fatal alert path. An empty Optional is
 * the canonical "user typed something wrong" signal that {@code LoginController}
 * routes to its inline danger-state UX (red borders + "Credenciales no
 * válidas." label) without ever reaching {@code GlobalExceptionHandler}.
 *
 * <p>Genuine infrastructure failures (DB unreachable, etc.) still throw —
 * those should reach the global handler.
 *
 * <p><b>ES — </b>Autentica a un usuario por username-o-email + contraseña.
 *
 * <p><b>Contrato Optional-vacío para credenciales inválidas.</b> Un
 * username/email o contraseña incorrectos resuelven a
 * {@link Optional#empty()} — <em>no</em> a una excepción. Es deliberado:
 * una excepción de autenticación fallida se filtraría a través del
 * handler global como traza y dispararía la ruta de alerta fatal hacia
 * el operador. Un Optional vacío es la señal canónica de "el usuario
 * tecleó algo mal" que {@code LoginController} enruta a su UX de estado
 * de peligro inline (bordes rojos + etiqueta "Credenciales no
 * válidas.") sin pasar nunca por {@code GlobalExceptionHandler}.
 *
 * <p>Los fallos de infraestructura genuinos (BD inalcanzable, etc.)
 * siguen lanzando excepción — esos sí deben llegar al handler global.
 */
public interface ILoginUseCase {

    Optional<UserResponse> execute(LoginRequest request);
}
