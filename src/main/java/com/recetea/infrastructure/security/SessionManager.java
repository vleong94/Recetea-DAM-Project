package com.recetea.infrastructure.security;

import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.user.domain.UserId;

import java.util.Optional;

public class SessionManager implements IUserSessionService {

    private UserId currentUserId;

    public void login(UserId userId) {
        this.currentUserId = userId;
    }

    public void logout() {
        this.currentUserId = null;
    }

    @Override
    public Optional<UserId> getCurrentUserId() {
        return Optional.ofNullable(currentUserId);
    }
}
