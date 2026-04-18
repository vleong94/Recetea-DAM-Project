package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.UnitId;

public class Unit {

    private final UnitId id;
    private final String name;
    private final String abbreviation;

    public Unit(UnitId id, String name, String abbreviation) {
        if (name == null || name.trim().isEmpty()) {
            throw new UnitValidationException("El nombre de la unidad es un campo obligatorio.");
        }
        if (abbreviation == null || abbreviation.trim().isEmpty()) {
            throw new UnitValidationException("La abreviatura de la unidad es un campo obligatorio.");
        }
        this.id = id;
        this.name = name.trim();
        this.abbreviation = abbreviation.trim();
    }

    public UnitId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    @Override
    public String toString() {
        return name + " (" + abbreviation + ")";
    }

    public static class UnitValidationException extends RuntimeException {
        public UnitValidationException(String message) {
            super(message);
        }
    }
}
