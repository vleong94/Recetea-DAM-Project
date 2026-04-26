package com.recetea.core.user.application.usecases;

import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.application.ports.in.IRegisterUserUseCase;
import com.recetea.core.user.application.ports.in.dto.RegisterUserRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.DuplicateIdentityException;
import com.recetea.core.user.domain.User;
import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            userRepository.save(user);

            log.info("User '{}' registered successfully. ID: {}", request.username(), user.getId().value());
            return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
        });
    }
}
