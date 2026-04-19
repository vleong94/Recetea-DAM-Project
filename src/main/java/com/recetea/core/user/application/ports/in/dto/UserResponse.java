package com.recetea.core.user.application.ports.in.dto;

import com.recetea.core.user.domain.UserId;

public record UserResponse(UserId id, String username, String email) {}
