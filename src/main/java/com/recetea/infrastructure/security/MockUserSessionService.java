package com.recetea.infrastructure.security;

import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.user.domain.UserId;

public class MockUserSessionService implements IUserSessionService {

    @Override
    public UserId getCurrentUserId() {
        return new UserId(1);
    }
}