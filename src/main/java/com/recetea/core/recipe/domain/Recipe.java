package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;
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
import java.util.Objects;

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
    private boolean metricsDirty = false;

    private final List<RecipeIngredient> ingredients = new ArrayList<>();
    private final List<RecipeStep> steps = new ArrayList<>();
    private final List<Rating> ratings = new ArrayList<>();
    private final List<RecipeMedia> mediaItems = new ArrayList<>();

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

    public void hydrateMedia(RecipeMedia media) {
        this.mediaItems.add(media);
    }

    public void addMedia(RecipeMedia media) {
        Objects.requireNonNull(media, "media no puede ser nulo.");
        if (mediaItems.isEmpty() || media.isMain()) {
            clearAllMainFlags();
            mediaItems.add(cloneWithIsMain(media, true));
        } else {
            mediaItems.add(media);
        }
    }

    public void setMainMedia(RecipeMediaId id) {
        Objects.requireNonNull(id, "id no puede ser nulo.");
        boolean found = mediaItems.stream().anyMatch(m -> id.equals(m.id()));
        if (!found) throw new RecipeValidationException("Recurso multimedia no encontrado con ID: " + id.value());
        for (int i = 0; i < mediaItems.size(); i++) {
            mediaItems.set(i, cloneWithIsMain(mediaItems.get(i), id.equals(mediaItems.get(i).id())));
        }
    }

    public void removeMedia(RecipeMediaId id) {
        Objects.requireNonNull(id, "id no puede ser nulo.");
        mediaItems.removeIf(m -> id.equals(m.id()));
    }

    private void clearAllMainFlags() {
        for (int i = 0; i < mediaItems.size(); i++) {
            RecipeMedia m = mediaItems.get(i);
            if (m.isMain()) mediaItems.set(i, cloneWithIsMain(m, false));
        }
    }

    private RecipeMedia cloneWithIsMain(RecipeMedia m, boolean isMain) {
        return new RecipeMedia(m.id(), m.recipeId(), m.storageKey(),
                m.storageProvider(), m.mimeType(), m.sizeBytes(), isMain, m.sortOrder());
    }

    public void addRating(UserId voterId, Score score, String comment) {
        if (voterId.equals(this.authorId))
            throw new RecipeValidationException("El autor no puede valorar su propia receta.");
        boolean alreadyRated = ratings.stream().anyMatch(r -> r.getUserId().equals(voterId));
        if (alreadyRated)
            throw new RecipeValidationException("El usuario ya ha valorado esta receta.");
        ratings.add(new Rating(voterId, score, comment, LocalDateTime.now()));
        recalculateSocialMetrics();
        this.metricsDirty = true;
    }

    private void recalculateSocialMetrics() {
        totalRatings = ratings.size();
        if (ratings.isEmpty()) {
            averageScore = BigDecimal.ZERO.setScale(2);
        } else {
            int sum = ratings.stream().mapToInt(r -> r.getScore().value()).sum();
            averageScore = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(totalRatings), 2, RoundingMode.HALF_UP);
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

    public List<RecipeMedia> getMediaItems() {
        return Collections.unmodifiableList(mediaItems);
    }

    public boolean isMetricsDirty() { return metricsDirty; }
    public void clearMetricsDirty() { this.metricsDirty = false; }

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
