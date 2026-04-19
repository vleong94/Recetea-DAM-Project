package com.recetea.core.shared.application.ports.in;

import com.recetea.core.user.domain.UserId;

import java.util.Optional;

public interface IUserSessionService {

    void login(UserId userId);
    void logout();
    Optional<UserId> getCurrentUserId();
}