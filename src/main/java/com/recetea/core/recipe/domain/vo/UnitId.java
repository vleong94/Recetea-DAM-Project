package com.recetea.core.recipe.domain.vo;

public record UnitId(int value) {
    public UnitId {
        if (value <= 0) throw new IllegalArgumentException("UnitId debe ser mayor que cero.");
    }
}
