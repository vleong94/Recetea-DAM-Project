package com.recetea.core.recipe.domain.vo;

public record Servings(int value) {
    public static final int MAX_SERVINGS = 1000;

    public Servings {
        if (value <= 0)
            throw new IllegalArgumentException("Las raciones deben ser mayores que cero.");
        if (value > MAX_SERVINGS)
            throw new IllegalArgumentException(
                    "Las raciones no pueden superar " + MAX_SERVINGS + ".");
    }
}
