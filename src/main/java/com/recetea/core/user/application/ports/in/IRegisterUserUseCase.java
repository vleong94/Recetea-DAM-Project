package com.recetea.core.user.application.ports.in;

import com.recetea.core.user.application.ports.in.dto.RegisterUserRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;

public interface IRegisterUserUseCase {

    UserResponse execute(RegisterUserRequest request);
}
