package com.recetea.core.recipe.domain;

public class RecipeNotFoundException extends RuntimeException {

    public RecipeNotFoundException(int id) {
        super("Receta no encontrada con ID: " + id);
    }
}
