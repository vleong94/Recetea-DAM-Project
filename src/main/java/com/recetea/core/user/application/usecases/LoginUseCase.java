package com.recetea.core.user.application.usecases;

import com.recetea.core.user.application.ports.in.ILoginUseCase;
import com.recetea.core.user.application.ports.in.dto.LoginRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.User;

import java.util.Optional;

public class LoginUseCase implements ILoginUseCase {

    private final IUserRepository userRepository;
    private final IPasswordEncoder passwordEncoder;

    public LoginUseCase(IUserRepository userRepository, IPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<UserResponse> execute(LoginRequest request) {
        Optional<User> user = userRepository.findByUsername(request.usernameOrEmail());
        if (user.isEmpty()) {
            user = userRepository.findByEmail(request.usernameOrEmail());
        }

        return user
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getEmail()));
    }
}
