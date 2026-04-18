package com.recetea.core.recipe.domain.vo;

public record UserId(int value) {
    public UserId {
        if (value <= 0) throw new IllegalArgumentException("UserId debe ser mayor que cero.");
    }
}
