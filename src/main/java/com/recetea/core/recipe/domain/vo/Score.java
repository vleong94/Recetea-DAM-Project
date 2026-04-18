package com.recetea.core.recipe.domain.vo;

public record Score(int value) {
    public Score {
        if (value < 1 || value > 5)
            throw new IllegalArgumentException("La puntuación debe estar entre 1 y 5.");
    }
}
