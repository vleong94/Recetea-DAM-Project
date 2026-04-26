package com.recetea.core.recipe.domain;

public class InvalidIngredientException extends RuntimeException {

    public InvalidIngredientException(String message) {
        super(message);
    }
}
