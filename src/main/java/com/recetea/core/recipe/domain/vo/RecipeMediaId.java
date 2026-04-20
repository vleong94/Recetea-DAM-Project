package com.recetea.core.recipe.domain.vo;

public record RecipeMediaId(int value) {
    public RecipeMediaId {
        if (value <= 0) throw new IllegalArgumentException("RecipeMediaId debe ser mayor que cero.");
    }
}
