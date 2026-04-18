package com.recetea.core.recipe.domain.vo;

public record IngredientId(int value) {
    public IngredientId {
        if (value <= 0) throw new IllegalArgumentException("IngredientId debe ser mayor que cero.");
    }
}
