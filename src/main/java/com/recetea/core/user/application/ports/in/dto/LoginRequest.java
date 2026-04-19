package com.recetea.core.user.application.ports.in.dto;

public record LoginRequest(String usernameOrEmail, String password) {}
