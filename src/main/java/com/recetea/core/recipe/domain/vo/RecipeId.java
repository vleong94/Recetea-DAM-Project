package com.recetea.core.recipe.domain.vo;

public record RecipeId(int value) {
    public RecipeId {
        if (value <= 0) throw new IllegalArgumentException("RecipeId debe ser mayor que cero.");
    }
}
