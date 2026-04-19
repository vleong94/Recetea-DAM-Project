package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Score;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.user.domain.UserId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

    private BigDecimal averageScore = BigDecimal.ZERO;
    private int totalRatings = 0;

    private final List<RecipeIngredient> ingredients = new ArrayList<>();
    private final List<RecipeStep> steps = new ArrayList<>();
    private final List<Rating> ratings = new ArrayList<>();

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
    public void setSocialMetrics(BigDecimal averageScore, int totalRatings) {
        this.averageScore = averageScore != null ? averageScore : BigDecimal.ZERO;
        this.totalRatings = totalRatings;
    }

    public void syncIngredients(List<RecipeIngredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty())
            throw new RecipeValidationException("La receta debe tener al menos un ingrediente.");
        this.ingredients.clear();
        newIngredients.stream().filter(i -> i != null).forEach(this.ingredients::add);
    }

    public void syncSteps(List<RecipeStep> newSteps) {
        if (newSteps == null || newSteps.isEmpty())
            throw new RecipeValidationException("La receta debe tener al menos un paso.");
        this.steps.clear();
        for (RecipeStep step : newSteps) {
            if (step == null) continue;
            boolean duplicateOrder = steps.stream().anyMatch(s -> s.stepOrder() == step.stepOrder());
            if (duplicateOrder) throw new RecipeValidationException("Orden de paso duplicado.");
            this.steps.add(step);
        }
        this.steps.sort(Comparator.comparingInt(RecipeStep::stepOrder));
    }

    public void hydrateRating(Rating rating) {
        this.ratings.add(rating);
    }

    public void addRating(UserId voterId, Score score, String comment) {
        if (voterId.equals(this.authorId))
            throw new RecipeValidationException("El autor no puede valorar su propia receta.");
        boolean alreadyRated = ratings.stream().anyMatch(r -> r.getUserId().equals(voterId));
        if (alreadyRated)
            throw new RecipeValidationException("El usuario ya ha valorado esta receta.");
        ratings.add(new Rating(voterId, score, comment, LocalDateTime.now()));
        recalculateSocialMetrics();
    }

    private void recalculateSocialMetrics() {
        totalRatings = ratings.size();
        if (ratings.isEmpty()) {
            averageScore = BigDecimal.ZERO;
        } else {
            double avg = ratings.stream().mapToInt(r -> r.getScore().value()).average().orElse(0.0);
            averageScore = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        }
    }

    public List<RecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public List<RecipeStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public List<Rating> getRatings() {
        return Collections.unmodifiableList(ratings);
    }

    public RecipeId getId() { return id; }
    public UserId getAuthorId() { return authorId; }
    public BigDecimal getAverageScore() { return averageScore; }
    public int getTotalRatings() { return totalRatings; }
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
