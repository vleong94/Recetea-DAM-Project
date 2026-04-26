package com.recetea.core.recipe.application.ports.in.dto;

public record SearchCriteria(
        String title,
        Integer maxPreparationTime,
        Integer minServings,
        String categoryName,
        String difficultyName,
        String ingredientName,
        String authorUsername
) {}
