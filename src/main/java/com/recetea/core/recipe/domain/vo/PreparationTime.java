package com.recetea.core.recipe.domain.vo;

public record PreparationTime(int value) {
    public static final int MAX_MINUTES = 43200; // 30 days

    public PreparationTime {
        if (value <= 0)
            throw new IllegalArgumentException("El tiempo de preparación debe ser mayor que cero.");
        if (value > MAX_MINUTES)
            throw new IllegalArgumentException(
                    "El tiempo de preparación no puede superar " + MAX_MINUTES + " minutos (30 días).");
    }
}
