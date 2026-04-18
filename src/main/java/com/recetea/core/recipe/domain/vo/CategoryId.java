package com.recetea.core.recipe.domain.vo;

public record CategoryId(int value) {
    public CategoryId {
        if (value <= 0) throw new IllegalArgumentException("CategoryId debe ser mayor que cero.");
    }
}
