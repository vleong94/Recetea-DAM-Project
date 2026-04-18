package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.user.domain.UserId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Recipe {

    private RecipeId id;
    private final UserId authorId;
    private Category category;
    private Difficulty difficulty;
    private String title;
    private String description;
    private PreparationTime preparationTimeMinutes;
    private Servings servings;

    private final List<RecipeIngredient> ingredients = new ArrayList<>();
    private final List<RecipeStep> steps = new ArrayList<>();

    public Recipe(UserId authorId, Category category, Difficulty difficulty,
                  String title, String description, PreparationTime preparationTimeMinutes, Servings servings) {
        this.authorId = authorId;
        this.category = category;
        this.difficulty = difficulty;
        this.title = title;
        this.description = description;
        this.preparationTimeMinutes = preparationTimeMinutes;
        this.servings = servings;
    }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPreparationTimeMinutes(PreparationTime minutes) { this.preparationTimeMinutes = minutes; }
    public void setServings(Servings servings) { this.servings = servings; }
    public void setCategory(Category category) { this.category = category; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public void setId(RecipeId id) { this.id = id; }

    public void syncIngredients(List<RecipeIngredient> newIngredients) {
        this.ingredients.clear();
        if (newIngredients != null) {
            newIngredients.stream().filter(i -> i != null).forEach(this.ingredients::add);
        }
    }

    public void syncSteps(List<RecipeStep> newSteps) {
        this.steps.clear();
        if (newSteps == null) return;
        for (RecipeStep step : newSteps) {
            if (step == null) continue;
            boolean duplicateOrder = steps.stream().anyMatch(s -> s.stepOrder() == step.stepOrder());
            if (duplicateOrder) throw new RecipeValidationException("Orden de paso duplicado.");
            this.steps.add(step);
        }
        this.steps.sort(Comparator.comparingInt(RecipeStep::stepOrder));
    }

    public List<RecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public List<RecipeStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public RecipeId getId() { return id; }
    public UserId getAuthorId() { return authorId; }
    public String getTitle() { return title; }
    public Category getCategory() { return category; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getDescription() { return description; }
    public PreparationTime getPreparationTimeMinutes() { return preparationTimeMinutes; }
    public Servings getServings() { return servings; }

    public static class RecipeValidationException extends RuntimeException {
        public RecipeValidationException(String message) { super(message); }
    }

    public static class InvalidRecipeMetricException extends RecipeValidationException {
        public InvalidRecipeMetricException(String message) { super(message); }
    }
}
