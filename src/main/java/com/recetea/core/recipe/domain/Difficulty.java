package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.DifficultyId;

public class Difficulty {

    private final DifficultyId id;
    private final String name;

    public Difficulty(DifficultyId id, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new DifficultyValidationException("El nombre de la dificultad es un campo obligatorio.");
        }
        this.id = id;
        this.name = name.trim();
    }

    public DifficultyId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static class DifficultyValidationException extends RuntimeException {
        public DifficultyValidationException(String message) {
            super(message);
        }
    }
}
