package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.IngredientId;

public class Ingredient {

    private final IngredientId id;
    private final CategoryId categoryId;
    private final String name;

    public Ingredient(IngredientId id, CategoryId categoryId, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IngredientValidationException("El nombre del ingrediente es un campo obligatorio.");
        }
        this.id = id;
        this.categoryId = categoryId;
        this.name = name.trim();
    }

    public IngredientId getId() {
        return id;
    }

    public CategoryId getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static class IngredientValidationException extends RuntimeException {
        public IngredientValidationException(String message) {
            super(message);
        }
    }
}
