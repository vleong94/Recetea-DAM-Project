package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.CategoryId;

public class Category {

    private final CategoryId id;
    private final String name;

    public Category(CategoryId id, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CategoryValidationException("El nombre de la categoría es obligatorio para su instanciación.");
        }
        this.id = id;
        this.name = name.trim();
    }

    public CategoryId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static class CategoryValidationException extends RuntimeException {
        public CategoryValidationException(String message) {
            super(message);
        }
    }
}
