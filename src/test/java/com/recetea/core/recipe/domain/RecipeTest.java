package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.shared.domain.ErrorCode;
import com.recetea.core.user.domain.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeTest {

    private Recipe createBaseRecipe() {
        return new Recipe(
                new UserId(1),
                new Category(new CategoryId(1), "Appetizers"),
                new Difficulty(new DifficultyId(1), "Easy"),
                "Test Recipe",
                "Test description",
                new PreparationTime(20),
                new Servings(2)
        );
    }

    private Recipe createRecipeWithId() {
        return new Recipe(
                new RecipeId(1), new UserId(1),
                new Category(new CategoryId(1), "Appetizers"),
                new Difficulty(new DifficultyId(1), "Easy"),
                "Test Recipe", "Test description",
                new PreparationTime(20), new Servings(2),
                BigDecimal.ZERO, 0);
    }

    @Test
    @DisplayName("Should detect and reject duplicate step orders")
    void shouldPreventDuplicateSteps() {
        Recipe recipe = createBaseRecipe();

        assertThrows(Recipe.RecipeValidationException.class, () ->
                recipe.syncSteps(List.of(
                        new RecipeStep(1, "Step A"),
                        new RecipeStep(1, "Step B")
                ))
        );
    }

    @Test
    @DisplayName("Should allow adding ingredients and reflect the correct count")
    void shouldAddIngredients() {
        Recipe updated = createBaseRecipe().syncIngredients(List.of(
                new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.valueOf(100))
        ));

        assertEquals(1, updated.getIngredients().size());
    }

    @Test
    @DisplayName("Should reject a null ingredient list")
    void shouldRejectNullIngredients() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.syncIngredients(null));
    }

    @Test
    @DisplayName("Should reject an empty ingredient list")
    void shouldRejectEmptyIngredients() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.syncIngredients(Collections.emptyList()));
    }

    @Test
    @DisplayName("Should reject duplicate IngredientIds and leave the aggregate untouched")
    void shouldRejectDuplicateIngredients() {
        Recipe recipe = createBaseRecipe();
        IngredientId duplicate = new IngredientId(7);

        InvalidRecipeDataException ex = assertThrows(InvalidRecipeDataException.class, () ->
                recipe.syncIngredients(List.of(
                        new RecipeIngredient(duplicate,         new UnitId(1), BigDecimal.valueOf(100)),
                        new RecipeIngredient(new IngredientId(8), new UnitId(1), BigDecimal.valueOf(50)),
                        new RecipeIngredient(duplicate,         new UnitId(2), BigDecimal.valueOf(25))
                ))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.errorCode());
        assertEquals(1, ex.errors().size(), "One duplicate should yield one accumulated error message");
        assertTrue(ex.errors().get(0).contains("Duplicate ingredient"),
                "Error message should mention the duplicate: " + ex.errors().get(0));
        assertTrue(recipe.getIngredients().isEmpty(),
                "Original aggregate must remain untouched (immutable record).");
    }

    @Test
    @DisplayName("Should reject a null step list")
    void shouldRejectNullSteps() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.syncSteps(null));
    }

    @Test
    @DisplayName("Should reject an empty step list")
    void shouldRejectEmptySteps() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.syncSteps(Collections.emptyList()));
    }

    @Test
    @DisplayName("Should prevent the author from rating their own recipe")
    void shouldRejectSelfRating() {
        Recipe recipe = createBaseRecipe();
        UserId authorId = new UserId(1);
        assertThrows(Recipe.RecipeValidationException.class,
                () -> recipe.addRating(authorId, new Score(5), "Excellent"));
    }

    @Test
    @DisplayName("Should allow a rating from another user")
    void shouldAllowRatingFromOtherUser() {
        Recipe rated = createBaseRecipe().addRating(new UserId(2), new Score(4), "Very good");
        assertEquals(1, rated.getRatings().size());
    }

    @Test
    @DisplayName("Should reject a duplicate rating from the same user")
    void shouldRejectDuplicateRatingFromSameUser() {
        UserId voter = new UserId(2);
        Recipe withRating = createBaseRecipe().addRating(voter, new Score(4), "Very good");
        assertThrows(Recipe.RecipeValidationException.class,
                () -> withRating.addRating(voter, new Score(3), "Trying again"));
    }

    @Test
    @DisplayName("Should update internal metrics when ratings are added")
    void shouldUpdateInternalMetricsWhenRatingIsAdded() {
        Recipe rated = createBaseRecipe()
                .addRating(new UserId(2), new Score(5), "Perfect")
                .addRating(new UserId(3), new Score(4), "Very good")
                .addRating(new UserId(4), new Score(3), "Decent");

        assertEquals(3, rated.getTotalRatings());
        assertEquals(0, BigDecimal.valueOf(4.00).setScale(2).compareTo(rated.getAverageScore()));
    }

    @Test
    @DisplayName("Social metrics should be preserved after syncing steps")
    void shouldMaintainMetricsAfterSyncSteps() {
        Recipe rated = createBaseRecipe().addRating(new UserId(2), new Score(5), "Perfect");

        BigDecimal scoreBefore = rated.getAverageScore();
        int totalBefore = rated.getTotalRatings();

        Recipe afterSync = rated.syncSteps(List.of(
                new RecipeStep(1, "New step"),
                new RecipeStep(2, "Another step")
        ));

        assertEquals(totalBefore, afterSync.getTotalRatings());
        assertEquals(0, scoreBefore.compareTo(afterSync.getAverageScore()));
    }

    @Test
    @DisplayName("averageScore should round correctly to 2 decimal places with HALF_UP")
    void shouldRoundAverageScoreToTwoDecimalPlaces() {
        // 5 + 5 + 4 = 14 / 3 = 4.6666... → HALF_UP → 4.67
        Recipe rated = createBaseRecipe()
                .addRating(new UserId(2), new Score(5), "Excellent")
                .addRating(new UserId(3), new Score(5), "Perfect")
                .addRating(new UserId(4), new Score(4), "Very good");

        assertEquals(3, rated.getTotalRatings());
        assertEquals(0, new BigDecimal("4.67").compareTo(rated.getAverageScore()),
                "14/3 rounded to 2 decimal places with HALF_UP should be 4.67");
        assertEquals(2, rated.getAverageScore().scale(),
                "averageScore should always have scale 2 for DB schema consistency");
    }

    @Test
    @DisplayName("Should throw an exception for negative preparation time")
    void shouldValidatePreparationTime() {
        assertThrows(IllegalArgumentException.class, () ->
                new Recipe(
                        new UserId(1),
                        new Category(new CategoryId(1), "A"),
                        new Difficulty(new DifficultyId(1), "B"),
                        "T", "D",
                        new PreparationTime(-10),
                        new Servings(2)
                )
        );
    }

    // -------------------------------------------------------------------------
    // Compact-constructor + wither validation (replaces former setter validation —
    // the immutable record routes every "field write" through the canonical
    // constructor, so withX(null) and the constructor-with-null tests assert the
    // same invariants the deleted setters used to.)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("withTitle(null) should throw RecipeValidationException via the compact constructor")
    void withTitle_ShouldThrow_WhenNull() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.withTitle(null));
    }

    @Test
    @DisplayName("withTitle(blank) should throw RecipeValidationException via the compact constructor")
    void withTitle_ShouldThrow_WhenBlank() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.withTitle("   "));
    }

    @Test
    @DisplayName("withDescription(null) should throw RecipeValidationException via the compact constructor")
    void withDescription_ShouldThrow_WhenNull() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.withDescription(null));
    }

    @Test
    @DisplayName("withCategory(null) should throw RecipeValidationException via the compact constructor")
    void withCategory_ShouldThrow_WhenNull() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.withCategory(null));
    }

    @Test
    @DisplayName("withDifficulty(null) should throw RecipeValidationException via the compact constructor")
    void withDifficulty_ShouldThrow_WhenNull() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.withDifficulty(null));
    }

    @Test
    @DisplayName("Create-path constructor should throw RecipeValidationException when title is blank")
    void constructor_ShouldThrow_WhenTitleIsBlank() {
        assertThrows(Recipe.RecipeValidationException.class, () ->
                new Recipe(new UserId(1),
                        new Category(new CategoryId(1), "Appetizers"),
                        new Difficulty(new DifficultyId(1), "Easy"),
                        "   ", "Valid description",
                        new PreparationTime(20), new Servings(2)));
    }

    @Test
    @DisplayName("Create-path constructor should throw RecipeValidationException when category is null")
    void constructor_ShouldThrow_WhenCategoryIsNull() {
        assertThrows(Recipe.RecipeValidationException.class, () ->
                new Recipe(new UserId(1),
                        null,
                        new Difficulty(new DifficultyId(1), "Easy"),
                        "Valid title", "Valid description",
                        new PreparationTime(20), new Servings(2)));
    }

    @Test
    @DisplayName("Create-path constructor should throw RecipeValidationException when difficulty is null")
    void constructor_ShouldThrow_WhenDifficultyIsNull() {
        assertThrows(Recipe.RecipeValidationException.class, () ->
                new Recipe(new UserId(1),
                        new Category(new CategoryId(1), "Appetizers"),
                        null,
                        "Valid title", "Valid description",
                        new PreparationTime(20), new Servings(2)));
    }

    @Test
    @DisplayName("Create-path constructor should throw RecipeValidationException when description is null")
    void constructor_ShouldThrow_WhenDescriptionIsNull() {
        assertThrows(Recipe.RecipeValidationException.class, () ->
                new Recipe(new UserId(1),
                        new Category(new CategoryId(1), "Appetizers"),
                        new Difficulty(new DifficultyId(1), "Easy"),
                        "Valid title", null,
                        new PreparationTime(20), new Servings(2)));
    }

    // -------------------------------------------------------------------------
    // Media management
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("First media item should be auto-promoted to isMain")
    void addMedia_ShouldAutoSetIsMain_WhenCollectionIsEmpty() {
        Recipe recipe = createRecipeWithId();
        RecipeMedia media = new RecipeMedia(null, recipe.getId(), "key/img.jpg", "LOCAL", "image/jpeg", 1024L, false, 0);

        Recipe withMedia = recipe.addMedia(media);

        assertEquals(1, withMedia.getMediaItems().size());
        assertTrue(withMedia.getMediaItems().get(0).isMain(), "First item should be promoted to isMain=true");
    }

    @Test
    @DisplayName("Adding a second non-main item should not change the existing isMain")
    void addMedia_ShouldNotChangeExistingMain_WhenNewMediaIsNotMain() {
        Recipe recipe = createRecipeWithId()
                .addMedia(new RecipeMedia(null, new RecipeId(1), "key/a.jpg", "LOCAL", "image/jpeg", 512L, false, 0))
                .addMedia(new RecipeMedia(null, new RecipeId(1), "key/b.jpg", "LOCAL", "image/jpeg", 512L, false, 1));

        assertTrue(recipe.getMediaItems().get(0).isMain(), "First item should remain main");
        assertFalse(recipe.getMediaItems().get(1).isMain(), "Second item should not be main");
    }

    @Test
    @DisplayName("setMainMedia should atomically transfer isMain and clear the previous flag")
    void setMainMedia_ShouldTransferIsMain_AtomicallyAndClearPrevious() {
        RecipeMediaId idA = new RecipeMediaId(10);
        RecipeMediaId idB = new RecipeMediaId(20);
        Recipe recipe = createRecipeWithId().withMediaItems(List.of(
                new RecipeMedia(idA, new RecipeId(1), "key/a.jpg", "LOCAL", "image/jpeg", 512L, true,  0),
                new RecipeMedia(idB, new RecipeId(1), "key/b.jpg", "LOCAL", "image/jpeg", 512L, false, 1)
        ));

        Recipe afterSwap = recipe.setMainMedia(idB);

        assertFalse(afterSwap.getMediaItems().stream().filter(m -> idA.equals(m.id())).findFirst().orElseThrow().isMain(),
                "Previous main should lose the flag");
        assertTrue(afterSwap.getMediaItems().stream().filter(m -> idB.equals(m.id())).findFirst().orElseThrow().isMain(),
                "New main should have the flag");
    }

    @Test
    @DisplayName("setMainMedia should throw RecipeValidationException when ID not found")
    void setMainMedia_ShouldThrow_WhenIdNotFound() {
        Recipe recipe = createRecipeWithId();
        assertThrows(Recipe.RecipeValidationException.class,
                () -> recipe.setMainMedia(new RecipeMediaId(999)));
    }

    @Test
    @DisplayName("removeMedia should remove the correct element from the collection")
    void removeMedia_ShouldRemoveCorrectElement() {
        RecipeMediaId idA = new RecipeMediaId(10);
        RecipeMediaId idB = new RecipeMediaId(20);
        Recipe recipe = createRecipeWithId().withMediaItems(List.of(
                new RecipeMedia(idA, new RecipeId(1), "key/a.jpg", "LOCAL", "image/jpeg", 512L, true,  0),
                new RecipeMedia(idB, new RecipeId(1), "key/b.jpg", "LOCAL", "image/jpeg", 512L, false, 1)
        ));

        Recipe afterRemove = recipe.removeMedia(idA);

        assertEquals(1, afterRemove.getMediaItems().size());
        assertEquals(idB, afterRemove.getMediaItems().get(0).id());
    }
}
