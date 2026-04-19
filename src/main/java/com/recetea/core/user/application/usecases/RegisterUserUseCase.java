package com.recetea.core.user.application.usecases;

import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.application.ports.in.IRegisterUserUseCase;
import com.recetea.core.user.application.ports.in.dto.RegisterUserRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.DuplicateIdentityException;
import com.recetea.core.user.domain.User;

public class RegisterUserUseCase implements IRegisterUserUseCase {

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
        return transactionManager.execute(() -> {
            if (userRepository.findByUsername(request.username()).isPresent())
                throw new DuplicateIdentityException(
                        "El nombre de usuario ya está en uso: " + request.username());

            if (userRepository.findByEmail(request.email()).isPresent())
                throw new DuplicateIdentityException(
                        "El email ya está registrado: " + request.email());

            String encodedPassword = passwordEncoder.encode(request.password());
            User user = new User(request.username(), request.email(), encodedPassword);
            userRepository.save(user);

            return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
        });
    }
}
