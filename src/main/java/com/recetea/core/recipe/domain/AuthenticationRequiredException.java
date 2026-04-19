package com.recetea.core.recipe.domain;

public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException() {
        super("Se requiere autenticación para realizar esta operación.");
    }
}
