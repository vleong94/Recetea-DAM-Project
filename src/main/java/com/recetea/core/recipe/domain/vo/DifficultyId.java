package com.recetea.core.recipe.domain.vo;

public record DifficultyId(int value) {
    public DifficultyId {
        if (value <= 0) throw new IllegalArgumentException("DifficultyId debe ser mayor que cero.");
    }
}
