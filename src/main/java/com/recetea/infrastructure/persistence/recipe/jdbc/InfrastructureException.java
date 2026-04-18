package com.recetea.infrastructure.persistence.recipe.jdbc;

public class InfrastructureException extends RuntimeException {
    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
