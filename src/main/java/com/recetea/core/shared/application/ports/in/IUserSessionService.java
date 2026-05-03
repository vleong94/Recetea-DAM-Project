package com.recetea.core.shared.application.ports.in;

import com.recetea.core.user.domain.UserId;

import java.util.Optional;

/**
 * Inbound port for the in-process session — who's currently logged in. The
 * implementation ({@code SessionManager}) holds a single mutable
 * {@code volatile UserId} so the FX thread (login/logout) and background
 * virtual threads (use-case handlers reading the session) see the same value
 * without the cost of full synchronization.
 *
 * <p>The session is intentionally process-local — there's no token,
 * no expiry, no remote validation. Closing the application logs the user out
 * implicitly.
 *
 * <p><b>ES — </b>Puerto de entrada para la sesión en proceso — quién
 * está logueado actualmente. La implementación
 * ({@code SessionManager}) mantiene un único {@code volatile UserId}
 * mutable, de modo que el hilo FX (login/logout) y los virtual threads
 * en segundo plano (handlers de casos de uso que leen la sesión) ven el
 * mismo valor sin el coste de la sincronización completa.
 *
 * <p>La sesión es deliberadamente local al proceso — no hay token, ni
 * expiración, ni validación remota. Cerrar la aplicación desloguea al
 * usuario implícitamente.
 */
public interface IUserSessionService {

    /**
     * Stores the active user. Called once after a successful {@code LoginUseCase} execution.
     *
     * <p><b>ES — </b>Guarda el usuario activo. Se llama una vez después
     * de una ejecución exitosa de {@code LoginUseCase}.
     */
    void login(UserId userId, String username);

    /**
     * Clears the active user. After this call {@link #getCurrentUserId()} returns empty.
     *
     * <p><b>ES — </b>Limpia el usuario activo. Después de esta llamada
     * {@link #getCurrentUserId()} devuelve vacío.
     */
    void logout();

    /**
     * Empty when no user is logged in (anonymous flows: login screen, register screen).
     *
     * <p><b>ES — </b>Vacío cuando no hay usuario logueado (flujos
     * anónimos: pantalla de login, pantalla de registro).
     */
    Optional<UserId> getCurrentUserId();

    /**
     * Convenience accessor — same lifecycle as {@link #getCurrentUserId()}.
     *
     * <p><b>ES — </b>Accesor de conveniencia — mismo ciclo de vida que
     * {@link #getCurrentUserId()}.
     */
    Optional<String> getCurrentUsername();
}
