package com.recetea.core.recipe.domain.vo;

public record PreparationTime(int value) {
    public PreparationTime {
        if (value <= 0) throw new IllegalArgumentException("El tiempo de preparación debe ser mayor que cero.");
    }
}
