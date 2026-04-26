package com.recetea.core.user.application.usecases;

import com.recetea.core.user.application.ports.in.ILoginUseCase;
import com.recetea.core.user.application.ports.in.dto.LoginRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
        log.info("Login attempt for identifier: '{}'", request.usernameOrEmail());

        Optional<User> user = userRepository.findByUsername(request.usernameOrEmail());
        if (user.isEmpty()) {
            user = userRepository.findByEmail(request.usernameOrEmail());
        }

        Optional<UserResponse> result = user
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getEmail()));

        if (result.isPresent()) {
            log.info("Login successful. User ID: {}", result.get().id().value());
        } else {
            log.warn("Login failed for identifier: '{}'", request.usernameOrEmail());
        }

        return result;
    }
}
