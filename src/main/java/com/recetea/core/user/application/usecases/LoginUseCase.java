package com.recetea.core.user.application.usecases;

import com.recetea.core.shared.domain.utils.MaskingUtils;
import com.recetea.core.user.application.ports.in.ILoginUseCase;
import com.recetea.core.user.application.ports.in.dto.LoginRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Authenticates a user by either username or email. Looks up the user
 * (probing by-username first, then by-email if the first probe misses), then
 * runs the BCrypt verify against the stored hash.
 *
 * <p><b>Empty Optional, not exception, on bad credentials.</b> Both "no user
 * with this identifier" and "user found but password mismatched" resolve to
 * {@link Optional#empty()} — the controller handles that as the canonical
 * "wrong username/password" UX. Throwing instead would push the failure
 * through the global handler's fatal-alert path, which would leak a stack
 * trace for what is normal user-level error behaviour.
 *
 * <p>Identifier values are masked through {@link MaskingUtils#mask} on every
 * log line — the WARN line on a failed attempt is therefore safe to emit at
 * INFO+ level without leaking PII into logs.
 *
 * <p>Account-harvesting protection: callers see the same empty-Optional
 * regardless of whether the user exists or the password is wrong, so a probe
 * cannot enumerate registered identifiers.
 *
 * <p><b>ES — </b>Autentica a un usuario por username o email. Busca al
 * usuario (sondeando primero por username, luego por email si el
 * primer sondeo no encuentra), y luego ejecuta el verify de BCrypt
 * contra el hash almacenado.
 *
 * <p><b>Optional vacío, no excepción, ante credenciales erróneas.</b>
 * Tanto "no hay usuario con este identificador" como "usuario
 * encontrado pero la contraseña no coincide" resuelven a
 * {@link Optional#empty()} — el controlador lo maneja como la UX
 * canónica de "username/contraseña incorrectos". Lanzar en su lugar
 * empujaría el fallo por la ruta de alerta fatal del handler global,
 * lo que filtraría una traza para lo que es un comportamiento de
 * error normal a nivel de usuario.
 *
 * <p>Los valores de identificador se enmascaran con
 * {@link MaskingUtils#mask} en cada línea de log — la línea WARN de
 * un intento fallido es por tanto segura de emitir a nivel INFO+
 * sin filtrar PII en los logs.
 *
 * <p>Protección contra account-harvesting: los llamadores ven el
 * mismo Optional vacío sin importar si el usuario existe o la
 * contraseña es errónea, así que un sondeo no puede enumerar
 * identificadores registrados.
 */
public class LoginUseCase implements ILoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginUseCase.class);

    private final IUserRepository userRepository;
    private final IPasswordEncoder passwordEncoder;

    public LoginUseCase(IUserRepository userRepository, IPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<UserResponse> execute(LoginRequest request) {
        log.info("Login attempt for identifier: '{}'", MaskingUtils.mask(request.usernameOrEmail()));

        Optional<User> user = userRepository.findByUsername(request.usernameOrEmail());
        if (user.isEmpty()) {
            user = userRepository.findByEmail(request.usernameOrEmail());
        }

        Optional<UserResponse> result = user
                .filter(u -> passwordEncoder.matches(request.password(), u.passwordHash().value()))
                .map(u -> new UserResponse(u.id(), u.username().value(), u.email().value()));

        if (result.isPresent()) {
            log.info("Login successful. User ID: {}", result.get().id().value());
        } else {
            log.warn("Login failed for identifier: '{}'", MaskingUtils.mask(request.usernameOrEmail()));
        }

        return result;
    }
}
