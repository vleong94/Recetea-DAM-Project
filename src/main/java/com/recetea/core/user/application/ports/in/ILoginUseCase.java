package com.recetea.core.user.application.ports.in;

import com.recetea.core.user.application.ports.in.dto.LoginRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;

import java.util.Optional;

public interface ILoginUseCase {

    Optional<UserResponse> execute(LoginRequest request);
}
