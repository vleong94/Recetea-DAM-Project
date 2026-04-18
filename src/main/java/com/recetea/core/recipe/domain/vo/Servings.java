package com.recetea.core.recipe.domain.vo;

public record Servings(int value) {
    public Servings {
        if (value <= 0) throw new IllegalArgumentException("Las raciones deben ser mayores que cero.");
    }
}
