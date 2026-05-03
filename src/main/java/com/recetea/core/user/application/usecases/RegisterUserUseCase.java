package com.recetea.core.user.application.usecases;

import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.application.ports.in.IRegisterUserUseCase;
import com.recetea.core.user.application.ports.in.dto.RegisterUserRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.DuplicateIdentityException;
import com.recetea.core.user.domain.InvalidUserDataException;
import com.recetea.core.user.domain.User;
import com.recetea.core.user.domain.UserId;
import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists a new user account: validate input → check uniqueness inside a
 * transaction → hash password → insert.
 *
 * <p>The two uniqueness probes (by username, by email) and the {@code save}
 * call are wrapped in one transaction so a parallel registration attempt
 * with the same email can't slip in between the probe and the insert. The
 * unique indexes on {@code users.username} / {@code users.email} are the
 * actual atomicity guarantee at the DB layer; the in-app probes give us
 * targeted error messages ("Username already taken" vs "Email already
 * registered") that a raw constraint violation would not.
 *
 * <p>BCrypt cost-12 hashing happens inside the transaction so the slow CPU
 * work isn't paid before the uniqueness check confirms the account is
 * actually new — saves ~300 ms per duplicate-registration attempt.
 *
 * <p>Throws {@code DuplicateIdentityException} (code {@code CONFLICT}) on
 * username or email collision. The {@code RegisterController} maps that to
 * the inline danger-state UX without going through the global handler.
 *
 * <p><b>ES — </b>Persiste una nueva cuenta de usuario: valida entrada
 * → comprueba unicidad dentro de una transacción → hashea la
 * contraseña → inserta.
 *
 * <p>Los dos sondeos de unicidad (por username, por email) y la
 * llamada {@code save} se envuelven en una sola transacción para que
 * un intento de registro paralelo con el mismo email no pueda
 * colarse entre el sondeo y el insert. Los índices únicos sobre
 * {@code users.username} / {@code users.email} son la verdadera
 * garantía de atomicidad en la capa de BD; los sondeos en la app
 * nos dan mensajes de error específicos ("Username ya en uso" vs
 * "Email ya registrado") que una violación de restricción cruda no
 * daría.
 *
 * <p>El hashing BCrypt cost-12 ocurre dentro de la transacción para
 * que el trabajo CPU lento no se pague antes de que la comprobación
 * de unicidad confirme que la cuenta es realmente nueva — ahorra
 * ~300 ms por intento de registro duplicado.
 *
 * <p>Lanza {@code DuplicateIdentityException} (código
 * {@code CONFLICT}) ante colisión de username o email.
 * {@code RegisterController} la mapea a la UX inline de estado de
 * peligro sin pasar por el handler global.
 */
public class RegisterUserUseCase implements IRegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserUseCase.class);

    private final IUserRepository userRepository;
    private final IPasswordEncoder passwordEncoder;
    private final ITransactionManager transactionManager;

    public RegisterUserUseCase(IUserRepository userRepository,
                               IPasswordEncoder passwordEncoder,
                               ITransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionManager = transactionManager;
    }

    @Override
    public UserResponse execute(RegisterUserRequest request) {
        log.info("Registering new user: '{}'", request.username());
        request.validate().getOrThrow(InvalidUserDataException::new);

        return transactionManager.execute(() -> {
            if (userRepository.findByUsername(request.username()).isPresent()) {
                log.warn("Registration failed — username already taken: '{}'", request.username());
                throw new DuplicateIdentityException("Username already taken: " + request.username());
            }

            if (userRepository.findByEmail(request.email()).isPresent()) {
                log.warn("Registration failed — email already registered: '{}'", request.email());
                throw new DuplicateIdentityException("Email already registered: " + request.email());
            }

            String encodedPassword = passwordEncoder.encode(request.password());
            User user = new User(
                    new Username(request.username()),
                    new Email(request.email()),
                    new PasswordHash(encodedPassword)
            );
            UserId newId = userRepository.save(user);

            log.info("User '{}' registered successfully. ID: {}", request.username(), newId.value());
            return new UserResponse(newId, user.username().value(), user.email().value());
        });
    }
}
