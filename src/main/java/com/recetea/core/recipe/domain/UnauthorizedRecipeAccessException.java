package com.recetea.core.recipe.domain;

public class UnauthorizedRecipeAccessException extends RuntimeException {

    public UnauthorizedRecipeAccessException(String message) {
        super(message);
    }
}
