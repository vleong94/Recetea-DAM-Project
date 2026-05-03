package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.InvalidRecipeDataException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.recipe.domain.vo.UnitId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateRecipeUseCase — Functional Validation and Authorship Security")
class UpdateRecipeUseCaseTest {

    @Mock private IRecipeRepository recipeRepository;
    @Mock private ICategoryRepository categoryRepository;
    @Mock private IDifficultyRepository difficultyRepository;
    @Mock private ITransactionManager transactionManager;
    @Mock private IUserSessionService sessionService;

    private UpdateRecipeUseCase useCase;

    private static final UserId     OWNER      = new UserId(1);
    private static final RecipeId   RECIPE_ID  = new RecipeId(10);
    private static final Category   CATEGORY   = new Category(new CategoryId(1), "Desserts");
    private static final Difficulty DIFFICULTY = new Difficulty(new DifficultyId(1), "Easy");

    @BeforeEach
    void setUp() {
        useCase = new UpdateRecipeUseCase(
                recipeRepository, categoryRepository, difficultyRepository,
                transactionManager, sessionService);
    }

    private void stubTransaction() {
        doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; })
                .when(transactionManager).execute(any(Runnable.class));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Recipe buildOwnerRecipe() {
        return new Recipe(
                RECIPE_ID, OWNER, CATEGORY, DIFFICULTY,
                "Original Recipe", "Original description",
                new PreparationTime(30), new Servings(4),
                java.math.BigDecimal.ZERO, 0);
    }

    private SaveRecipeRequest validRequest() {
        // Request values are intentionally different from the original recipe so
        // mutation testing can observe whether each setter was actually invoked.
        return new SaveRecipeRequest(
                new CategoryId(2), new DifficultyId(2),
                "Updated Title", "New valid description",
                60, 8,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.TEN, "Flour", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "Mix everything well.")));
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("execute: throws InvalidRecipeDataException when title is blank")
    void execute_ShouldThrow_WhenTitleIsBlank() {
        SaveRecipeRequest request = new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1),
                "   ", "Valid description", 30, 4,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.ONE, "Salt", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "Single step")));

        InvalidRecipeDataException ex = assertThrows(InvalidRecipeDataException.class,
                () -> useCase.execute(RECIPE_ID, request));

        assertFalse(ex.getErrors().isEmpty(), "Expected at least one validation error");
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Title")),
                "Error message must mention the title field");
        // Validation fires before any repository or transaction involvement
        verify(transactionManager, never()).execute(any(Runnable.class));
        verify(recipeRepository, never()).update(any());
    }

    @Test
    @DisplayName("execute: accumulates all errors without short-circuit when multiple fields are invalid")
    void execute_ShouldAccumulateAllErrors_WhenMultipleFieldsAreInvalid() {
        // title blank, description blank, prep time = 0, servings = 0, no ingredients, no steps
        SaveRecipeRequest request = new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1),
                "", "", 0, 0,
                List.of(), List.of());

        InvalidRecipeDataException ex = assertThrows(InvalidRecipeDataException.class,
                () -> useCase.execute(RECIPE_ID, request));

        assertTrue(ex.getErrors().size() >= 4,
                "Expected at least 4 simultaneous errors; got: " + ex.getErrors());
        verify(transactionManager, never()).execute(any(Runnable.class));
        verify(recipeRepository, never()).update(any());
    }

    @Test
    @DisplayName("execute: throws AuthenticationRequiredException when no active session")
    void execute_ShouldThrow_AuthenticationRequiredException_WhenNoSession() {
        stubTransaction();
        // findById is reached before the session check; category/difficulty are not needed yet
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(buildOwnerRecipe()));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthenticationRequiredException.class,
                () -> useCase.execute(RECIPE_ID, validRequest()));
        verify(recipeRepository, never()).update(any());
    }

    @Test
    @DisplayName("execute: persists the updated aggregate when the request is valid")
    void execute_ShouldUpdateRecipe_WhenRequestIsValid() {
        stubTransaction();
        Recipe recipe = buildOwnerRecipe();
        // Distinct instances so an assertSame failure proves the setter was invoked
        Category   updatedCategory   = new Category(new CategoryId(2), "Salads");
        Difficulty updatedDifficulty = new Difficulty(new DifficultyId(2), "Hard");
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(recipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OWNER));
        when(categoryRepository.findById(new CategoryId(2))).thenReturn(Optional.of(updatedCategory));
        when(difficultyRepository.findById(new DifficultyId(2))).thenReturn(Optional.of(updatedDifficulty));

        assertDoesNotThrow(() -> useCase.execute(RECIPE_ID, validRequest()));

        // Recipe is now an immutable record — UpdateRecipeUseCase chains withers/syncs
        // off the loaded aggregate and persists the resulting new instance. Capture
        // it from the mock so the assertions inspect the post-update state.
        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).update(captor.capture());
        Recipe updated = captor.getValue();
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("New valid description", updated.getDescription());
        assertEquals(60, updated.getPreparationTimeMinutes().value());
        assertEquals(8, updated.getServings().value());
        assertSame(updatedCategory, updated.getCategory());
        assertSame(updatedDifficulty, updated.getDifficulty());
        assertEquals(1, updated.getIngredients().size());
        assertEquals(1, updated.getSteps().size());
    }

    @Test
    @DisplayName("execute: throws InvalidRecipeDataException when a step instruction is blank")
    void execute_ShouldThrow_WhenStepInstructionIsBlank() {
        SaveRecipeRequest request = new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1),
                "Valid title", "Valid description", 30, 4,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.ONE, "Salt", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "  ")));

        InvalidRecipeDataException ex = assertThrows(InvalidRecipeDataException.class,
                () -> useCase.execute(RECIPE_ID, request));

        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("instruction") || e.contains("Step")),
                "Error message must mention the step or its instruction");
    }
}
