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
import com.recetea.core.recipe.domain.vo.RecipeId;
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
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateRecipeUseCase — Functional Validation and Orchestration")
class CreateRecipeUseCaseTest {

    @Mock private IRecipeRepository recipeRepository;
    @Mock private ICategoryRepository categoryRepository;
    @Mock private IDifficultyRepository difficultyRepository;
    @Mock private ITransactionManager transactionManager;
    @Mock private IUserSessionService sessionService;

    private CreateRecipeUseCase useCase;

    private static final UserId   AUTHOR     = new UserId(1);
    private static final Category CATEGORY   = new Category(new CategoryId(1), "Postres");
    private static final Difficulty DIFFICULTY = new Difficulty(new DifficultyId(1), "Fácil");

    @BeforeEach
    void setUp() {
        useCase = new CreateRecipeUseCase(
                recipeRepository, categoryRepository, difficultyRepository,
                transactionManager, sessionService);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SaveRecipeRequest validRequest() {
        return new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1),
                "Tarta de Manzana", "Deliciosa tarta casera",
                45, 6,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.ONE, "Manzana", "ud")),
                List.of(new SaveRecipeRequest.StepRequest(1, "Pelar y trocear las manzanas.")));
    }

    private void stubHappyPath() {
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(AUTHOR));
        when(categoryRepository.findById(new CategoryId(1))).thenReturn(Optional.of(CATEGORY));
        when(difficultyRepository.findById(new DifficultyId(1))).thenReturn(Optional.of(DIFFICULTY));
        when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
        doAnswer(inv -> { ((Recipe) inv.getArgument(0)).setId(new RecipeId(99)); return null; })
                .when(recipeRepository).save(any(Recipe.class));
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("execute: happy path — persists the aggregate with the active session's author")
    void execute_ShouldSaveRecipe_WhenRequestIsValid() {
        stubHappyPath();

        RecipeId result = useCase.execute(validRequest());

        assertNotNull(result);
        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        Recipe saved = captor.getValue();
        assertEquals(AUTHOR, saved.getAuthorId());
        assertEquals("Tarta de Manzana", saved.getTitle());
        assertEquals(1, saved.getIngredients().size());
        assertEquals(1, saved.getSteps().size());
    }

    @Test
    @DisplayName("execute: throws InvalidRecipeDataException when title is blank")
    void execute_ShouldThrow_WhenTitleIsBlank() {
        SaveRecipeRequest request = new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1),
                "  ", "Descripción válida", 30, 4,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.ONE, "Sal", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "Paso único")));

        InvalidRecipeDataException ex = assertThrows(InvalidRecipeDataException.class,
                () -> useCase.execute(request));

        assertFalse(ex.getErrors().isEmpty(), "Expected at least one validation error");
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("título")),
                "Error message must mention the title field");
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
                () -> useCase.execute(request));

        assertTrue(ex.getErrors().size() >= 4,
                "Expected at least 4 simultaneous errors; got: " + ex.getErrors());
        // No repository interaction should have occurred
        verify(transactionManager, never()).execute(any(Supplier.class));
        verify(transactionManager, never()).execute(any(Runnable.class));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    @DisplayName("execute: throws AuthenticationRequiredException when no active session")
    void execute_ShouldThrow_AuthenticationRequiredException_WhenNoSession() {
        // Categories and difficulties are resolved before the session check inside the transaction
        when(categoryRepository.findById(new CategoryId(1))).thenReturn(Optional.of(CATEGORY));
        when(difficultyRepository.findById(new DifficultyId(1))).thenReturn(Optional.of(DIFFICULTY));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.empty());
        when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());

        assertThrows(AuthenticationRequiredException.class, () -> useCase.execute(validRequest()));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    @DisplayName("execute: throws InvalidRecipeDataException when an ingredient quantity is zero")
    void execute_ShouldThrow_WhenIngredientQuantityIsZero() {
        SaveRecipeRequest request = new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1),
                "Título válido", "Descripción válida", 30, 4,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.ZERO, "Sal", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "Paso único")));

        InvalidRecipeDataException ex = assertThrows(InvalidRecipeDataException.class,
                () -> useCase.execute(request));

        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("cantidad")),
                "Error message must mention the ingredient quantity");
    }
}
