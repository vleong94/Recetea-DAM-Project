package com.recetea.infrastructure.security;

import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.user.domain.UserId;

import java.util.Optional;

/**
 * In-process session holder. The single mutable state is two
 * {@code volatile} references — set by {@link #login} on the FX
 * thread, read from background virtual threads handling use-case
 * execution.
 *
 * <p><b>Why volatile is sufficient.</b> Login/logout are the only
 * writers, both happen on the FX thread, and readers never compose
 * the two fields under load — they read once at the top of a
 * use-case scope and use the snapshot for the rest of the call.
 * Full synchronization would add overhead with no observable
 * benefit; the {@code volatile} read provides the memory-barrier
 * visibility guarantee we actually need.
 *
 * <p>The session is process-local — there's no token, no expiry,
 * no remote validation. Closing the application logs the user out
 * implicitly (the JVM exit clears all state).
 *
 * <p><b>ES — </b>Contenedor de sesión en proceso. El único estado
 * mutable son dos referencias {@code volatile} — las pone
 * {@link #login} en el hilo FX y las leen los virtual threads en
 * segundo plano que manejan la ejecución de los casos de uso.
 *
 * <p><b>Por qué volatile basta.</b> Login/logout son los únicos
 * que escriben, ambos ocurren en el hilo FX, y los lectores nunca
 * componen los dos campos bajo carga — los leen una vez al
 * principio del ámbito del caso de uso y usan la instantánea
 * durante el resto de la llamada. La sincronización completa
 * añadiría sobrecoste sin beneficio observable; la lectura
 * {@code volatile} proporciona la garantía de visibilidad por
 * barrera de memoria que realmente necesitamos.
 *
 * <p>La sesión es local al proceso — sin token, sin expiración,
 * sin validación remota. Cerrar la aplicación desloguea al usuario
 * de forma implícita (la salida de la JVM limpia todo el estado).
 */
public class SessionManager implements IUserSessionService {

    private volatile UserId currentUserId;
    private volatile String currentUsername;

    @Override
    public void login(UserId userId, String username) {
        this.currentUserId   = userId;
        this.currentUsername = username;
    }

    @Override
    public void logout() {
        this.currentUserId   = null;
        this.currentUsername = null;
    }

    @Override
    public Optional<UserId> getCurrentUserId() {
        return Optional.ofNullable(currentUserId);
    }

    @Override
    public Optional<String> getCurrentUsername() {
        return Optional.ofNullable(currentUsername);
    }
}
