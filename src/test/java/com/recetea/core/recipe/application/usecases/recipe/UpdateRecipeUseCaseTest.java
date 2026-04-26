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
    private static final Category   CATEGORY   = new Category(new CategoryId(1), "Postres");
    private static final Difficulty DIFFICULTY = new Difficulty(new DifficultyId(1), "Fácil");

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
        Recipe recipe = new Recipe(
                OWNER, CATEGORY, DIFFICULTY,
                "Receta Original", "Descripción original",
                new PreparationTime(30), new Servings(4));
        recipe.setId(RECIPE_ID);
        return recipe;
    }

    private SaveRecipeRequest validRequest() {
        return new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1),
                "Título Actualizado", "Nueva descripción válida",
                60, 4,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.TEN, "Harina", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "Mezclar todo bien.")));
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("execute: throws InvalidRecipeDataException when title is blank")
    void execute_ShouldThrow_WhenTitleIsBlank() {
        SaveRecipeRequest request = new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1),
                "   ", "Descripción válida", 30, 4,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.ONE, "Sal", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "Paso único")));

        InvalidRecipeDataException ex = assertThrows(InvalidRecipeDataException.class,
                () -> useCase.execute(RECIPE_ID, request));

        assertFalse(ex.getErrors().isEmpty(), "Expected at least one validation error");
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("título")),
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
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(recipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OWNER));
        when(categoryRepository.findById(new CategoryId(1))).thenReturn(Optional.of(CATEGORY));
        when(difficultyRepository.findById(new DifficultyId(1))).thenReturn(Optional.of(DIFFICULTY));

        assertDoesNotThrow(() -> useCase.execute(RECIPE_ID, validRequest()));

        verify(recipeRepository).update(recipe);
        assertEquals("Título Actualizado", recipe.getTitle());
        assertEquals(60, recipe.getPreparationTimeMinutes().value());
    }

    @Test
    @DisplayName("execute: throws InvalidRecipeDataException when a step instruction is blank")
    void execute_ShouldThrow_WhenStepInstructionIsBlank() {
        SaveRecipeRequest request = new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1),
                "Título válido", "Descripción válida", 30, 4,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.ONE, "Sal", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "  ")));

        InvalidRecipeDataException ex = assertThrows(InvalidRecipeDataException.class,
                () -> useCase.execute(RECIPE_ID, request));

        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("instrucción") || e.contains("paso")),
                "Error message must mention the step or its instruction");
    }
}
