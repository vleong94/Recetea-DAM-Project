package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;
import com.recetea.core.recipe.domain.vo.Score;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.shared.domain.DomainException;
import com.recetea.core.shared.domain.ErrorCode;
import com.recetea.core.user.domain.UserId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Recipe aggregate — Java 24 record. Every component is final; mutation is expressed
 * by returning a new {@code Recipe} (see {@link #syncIngredients}, {@link #addRating},
 * the {@code with*} family, etc.).
 *
 * <p>The compact constructor enforces fail-fast invariants and converts every child
 * collection into an unmodifiable copy, so external callers cannot smuggle a mutable
 * list into the aggregate. {@code id} may be {@code null} on the create path
 * (populated by the persistence layer through {@link #withId} after insert).
 */
public record Recipe(
        RecipeId id,
        UserId authorId,
        Category category,
        Difficulty difficulty,
        String title,
        String description,
        PreparationTime preparationTimeMinutes,
        Servings servings,
        BigDecimal averageScore,
        int totalRatings,
        List<RecipeIngredient> ingredients,
        List<RecipeStep> steps,
        List<Rating> ratings,
        List<RecipeMedia> mediaItems
) {

    /**
     * Compact constructor — validates required components and defensively copies every
     * child collection into an unmodifiable form. Null collections collapse to
     * {@link List#of()}; null {@code averageScore} collapses to {@link BigDecimal#ZERO}.
     */
    public Recipe {
        if (category == null)
            throw new RecipeValidationException("Category must not be null.");
        if (difficulty == null)
            throw new RecipeValidationException("Difficulty must not be null.");
        if (title == null || title.isBlank())
            throw new RecipeValidationException("Title must not be blank.");
        if (description == null)
            throw new RecipeValidationException("Description must not be null.");

        averageScore = (averageScore == null) ? BigDecimal.ZERO : averageScore;
        ingredients  = (ingredients  == null) ? List.of() : List.copyOf(ingredients);
        steps        = (steps        == null) ? List.of() : List.copyOf(steps);
        ratings      = (ratings      == null) ? List.of() : List.copyOf(ratings);
        mediaItems   = (mediaItems   == null) ? List.of() : List.copyOf(mediaItems);
    }

    /** Create-path constructor — used by use cases when instantiating a fresh recipe. */
    public Recipe(UserId authorId, Category category, Difficulty difficulty,
                  String title, String description,
                  PreparationTime preparationTimeMinutes, Servings servings) {
        this(null, authorId, category, difficulty, title, description,
                preparationTimeMinutes, servings,
                BigDecimal.ZERO, 0,
                List.of(), List.of(), List.of(), List.of());
    }

    /** Reconstitution constructor — empty child collections; callers add children via withers. */
    public Recipe(RecipeId id, UserId authorId, Category category, Difficulty difficulty,
                  String title, String description,
                  PreparationTime preparationTimeMinutes, Servings servings,
                  BigDecimal averageScore, int totalRatings) {
        this(id, authorId, category, difficulty, title, description,
                preparationTimeMinutes, servings,
                averageScore, totalRatings,
                List.of(), List.of(), List.of(), List.of());
    }

    // ── Get-style accessor aliases ────────────────────────────────────────────
    // Records auto-generate the {@code id()}, {@code title()}, … accessors. The
    // {@code getXxx()} forms below are one-line bridges kept for backwards
    // compatibility with pre-record call-sites — both forms return the same value.

    public RecipeId               getId()                     { return id; }
    public UserId                 getAuthorId()               { return authorId; }
    public Category               getCategory()               { return category; }
    public Difficulty             getDifficulty()             { return difficulty; }
    public String                 getTitle()                  { return title; }
    public String                 getDescription()            { return description; }
    public PreparationTime        getPreparationTimeMinutes() { return preparationTimeMinutes; }
    public Servings               getServings()               { return servings; }
    public BigDecimal             getAverageScore()           { return averageScore; }
    public int                    getTotalRatings()           { return totalRatings; }
    public List<RecipeIngredient> getIngredients()            { return ingredients; }
    public List<RecipeStep>       getSteps()                  { return steps; }
    public List<Rating>           getRatings()                { return ratings; }
    public List<RecipeMedia>      getMediaItems()             { return mediaItems; }

    // ── Business operations (return new Recipe) ───────────────────────────────

    /**
     * Returns a new {@code Recipe} whose ingredient list is replaced. Validates the
     * incoming collection in full <em>before</em> constructing the replacement, so a
     * failure leaves the original aggregate untouched.
     */
    public Recipe syncIngredients(List<RecipeIngredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) {
            throw new RecipeValidationException("Recipe must have at least one ingredient.");
        }
        List<RecipeIngredient> validated = newIngredients.stream()
                .filter(Objects::nonNull)
                .toList();
        if (validated.isEmpty()) {
            throw new RecipeValidationException("Recipe must have at least one ingredient.");
        }
        Set<IngredientId> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (RecipeIngredient ri : validated) {
            IngredientId rid = ri.ingredientId();
            if (rid != null && !seen.add(rid)) {
                String name = ri.ingredientName() != null ? ri.ingredientName() : ("ID " + rid.value());
                duplicates.add("Duplicate ingredient '" + name + "'. Each ingredient may appear only once per recipe.");
            }
        }
        if (!duplicates.isEmpty()) {
            throw new InvalidRecipeDataException(duplicates);
        }
        return withIngredients(validated);
    }

    /**
     * Returns a new {@code Recipe} whose step list is replaced — sorted ascending by
     * {@code stepOrder}, with duplicate orders rejected. All-or-nothing: the new list
     * is fully validated before any object is constructed.
     */
    public Recipe syncSteps(List<RecipeStep> newSteps) {
        if (newSteps == null || newSteps.isEmpty()) {
            throw new RecipeValidationException("Recipe must have at least one step.");
        }
        List<RecipeStep> validated = newSteps.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(RecipeStep::stepOrder))
                .toList();
        if (validated.isEmpty()) {
            throw new RecipeValidationException("Recipe must have at least one step.");
        }
        for (int i = 1; i < validated.size(); i++) {
            if (validated.get(i).stepOrder() == validated.get(i - 1).stepOrder()) {
                throw new RecipeValidationException("Duplicate step order.");
            }
        }
        return withSteps(validated);
    }

    /**
     * Returns a new {@code Recipe} with the rating appended and social metrics
     * recomputed. Enforces the no-self-rating + at-most-one-rating-per-user invariants.
     */
    public Recipe addRating(UserId voterId, Score score, String comment) {
        if (voterId.equals(this.authorId)) {
            throw new RecipeValidationException("Author cannot rate their own recipe.");
        }
        if (ratings.stream().anyMatch(r -> r.userId().equals(voterId))) {
            throw new RecipeValidationException("User has already rated this recipe.");
        }
        List<Rating> newRatings = new ArrayList<>(ratings);
        newRatings.add(new Rating(voterId, score, comment, LocalDateTime.now()));
        SocialMetrics m = SocialMetrics.from(newRatings);
        return new Recipe(id, authorId, category, difficulty, title, description,
                preparationTimeMinutes, servings,
                m.averageScore(), m.totalRatings(),
                ingredients, steps, newRatings, mediaItems);
    }

    /**
     * Returns a new {@code Recipe} with {@code media} appended. The first item in an
     * empty collection is auto-promoted to {@code isMain=true}; an explicit
     * {@code isMain=true} on a subsequent insert clears the flag from any prior holder.
     */
    public Recipe addMedia(RecipeMedia media) {
        Objects.requireNonNull(media, "media must not be null.");
        List<RecipeMedia> updated = new ArrayList<>(mediaItems.size() + 1);
        if (mediaItems.isEmpty() || media.isMain()) {
            for (RecipeMedia m : mediaItems) {
                updated.add(m.isMain() ? m.withIsMain(false) : m);
            }
            updated.add(media.withIsMain(true));
        } else {
            updated.addAll(mediaItems);
            updated.add(media);
        }
        return withMediaItems(updated);
    }

    /** Returns a new {@code Recipe} with {@code targetId} as the sole {@code isMain=true} item. */
    public Recipe setMainMedia(RecipeMediaId targetId) {
        Objects.requireNonNull(targetId, "id must not be null.");
        if (mediaItems.stream().noneMatch(m -> targetId.equals(m.id()))) {
            throw new RecipeValidationException("Media item not found with ID: " + targetId.value());
        }
        List<RecipeMedia> updated = mediaItems.stream()
                .map(m -> m.withIsMain(targetId.equals(m.id())))
                .toList();
        return withMediaItems(updated);
    }

    /** Returns a new {@code Recipe} with the matching media item removed. */
    public Recipe removeMedia(RecipeMediaId targetId) {
        Objects.requireNonNull(targetId, "id must not be null.");
        List<RecipeMedia> updated = mediaItems.stream()
                .filter(m -> !targetId.equals(m.id()))
                .toList();
        return withMediaItems(updated);
    }

    /** Returns a new {@code Recipe} whose social metrics are recomputed from the current ratings list. */
    public Recipe recalculateSocialMetrics() {
        SocialMetrics m = SocialMetrics.from(ratings);
        return withSocialMetrics(m.averageScore(), m.totalRatings());
    }

    // ── Withers (evolutionary copy) ──────────────────────────────────────────

    public Recipe withId(RecipeId newId) {
        return new Recipe(newId, authorId, category, difficulty, title, description,
                preparationTimeMinutes, servings, averageScore, totalRatings,
                ingredients, steps, ratings, mediaItems);
    }

    public Recipe withTitle(String newTitle) {
        return new Recipe(id, authorId, category, difficulty, newTitle, description,
                preparationTimeMinutes, servings, averageScore, totalRatings,
                ingredients, steps, ratings, mediaItems);
    }

    public Recipe withDescription(String newDescription) {
        return new Recipe(id, authorId, category, difficulty, title, newDescription,
                preparationTimeMinutes, servings, averageScore, totalRatings,
                ingredients, steps, ratings, mediaItems);
    }

    public Recipe withCategory(Category newCategory) {
        return new Recipe(id, authorId, newCategory, difficulty, title, description,
                preparationTimeMinutes, servings, averageScore, totalRatings,
                ingredients, steps, ratings, mediaItems);
    }

    public Recipe withDifficulty(Difficulty newDifficulty) {
        return new Recipe(id, authorId, category, newDifficulty, title, description,
                preparationTimeMinutes, servings, averageScore, totalRatings,
                ingredients, steps, ratings, mediaItems);
    }

    public Recipe withPreparationTime(PreparationTime newPreparationTime) {
        return new Recipe(id, authorId, category, difficulty, title, description,
                newPreparationTime, servings, averageScore, totalRatings,
                ingredients, steps, ratings, mediaItems);
    }

    public Recipe withServings(Servings newServings) {
        return new Recipe(id, authorId, category, difficulty, title, description,
                preparationTimeMinutes, newServings, averageScore, totalRatings,
                ingredients, steps, ratings, mediaItems);
    }

    public Recipe withIngredients(List<RecipeIngredient> newIngredients) {
        return new Recipe(id, authorId, category, difficulty, title, description,
                preparationTimeMinutes, servings, averageScore, totalRatings,
                newIngredients, steps, ratings, mediaItems);
    }

    public Recipe withSteps(List<RecipeStep> newSteps) {
        return new Recipe(id, authorId, category, difficulty, title, description,
                preparationTimeMinutes, servings, averageScore, totalRatings,
                ingredients, newSteps, ratings, mediaItems);
    }

    public Recipe withRatings(List<Rating> newRatings) {
        return new Recipe(id, authorId, category, difficulty, title, description,
                preparationTimeMinutes, servings, averageScore, totalRatings,
                ingredients, steps, newRatings, mediaItems);
    }

    public Recipe withMediaItems(List<RecipeMedia> newMediaItems) {
        return new Recipe(id, authorId, category, difficulty, title, description,
                preparationTimeMinutes, servings, averageScore, totalRatings,
                ingredients, steps, ratings, newMediaItems);
    }

    public Recipe withSocialMetrics(BigDecimal newAverageScore, int newTotalRatings) {
        return new Recipe(id, authorId, category, difficulty, title, description,
                preparationTimeMinutes, servings, newAverageScore, newTotalRatings,
                ingredients, steps, ratings, mediaItems);
    }

    public static class RecipeValidationException extends DomainException {
        public RecipeValidationException(String message) {
            super(ErrorCode.VALIDATION_ERROR, message);
        }
    }
}
