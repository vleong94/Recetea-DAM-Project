package com.recetea.core.shared.application.ports.in;

import com.recetea.core.user.domain.UserId;

public interface IUserSessionService {

    UserId getCurrentUserId();
}