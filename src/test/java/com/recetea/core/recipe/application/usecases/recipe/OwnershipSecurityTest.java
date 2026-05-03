package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.*;
import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.shared.application.ConcurrencyGuard;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;
import com.recetea.core.user.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Ownership Security — Create, Update, and Delete")
class OwnershipSecurityTest {

    @Mock private IRecipeRepository recipeRepository;
    @Mock private IFavoriteRepository favoriteRepository;
    @Mock private ICategoryRepository categoryRepository;
    @Mock private IDifficultyRepository difficultyRepository;
    @Mock private ITransactionManager transactionManager;
    @Mock private IUserSessionService sessionService;

    private CreateRecipeUseCase createUseCase;
    private UpdateRecipeUseCase updateUseCase;
    private DeleteRecipeUseCase deleteUseCase;

    private Recipe ownerRecipe;
    private static final UserId OWNER = new UserId(1);
    private static final UserId INTRUDER = new UserId(2);
    private static final RecipeId RECIPE_ID = new RecipeId(10);

    @BeforeEach
    void setUp() {
        createUseCase = new CreateRecipeUseCase(
                recipeRepository, categoryRepository, difficultyRepository, transactionManager, sessionService,
                new ConcurrencyGuard(Integer.MAX_VALUE));
        updateUseCase = new UpdateRecipeUseCase(
                recipeRepository, categoryRepository, difficultyRepository, transactionManager, sessionService);
        deleteUseCase = new DeleteRecipeUseCase(recipeRepository, favoriteRepository, transactionManager, sessionService);

        ownerRecipe = new Recipe(
                RECIPE_ID, OWNER,
                new Category(new CategoryId(1), "Appetizers"),
                new Difficulty(new DifficultyId(1), "Easy"),
                "Owner's Recipe", "Description",
                new PreparationTime(20), new Servings(2),
                java.math.BigDecimal.ZERO, 0);

        doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; })
                .when(transactionManager).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("create: assigns the recipe to the active session user, not the payload")
    void create_assignsAuthorFromSession() {
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OWNER));
        when(categoryRepository.findById(new CategoryId(1)))
                .thenReturn(Optional.of(new Category(new CategoryId(1), "Appetizers")));
        when(difficultyRepository.findById(new DifficultyId(1)))
                .thenReturn(Optional.of(new Difficulty(new DifficultyId(1), "Easy")));
        when(transactionManager.execute(any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, java.util.function.Supplier.class).get());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(new RecipeId(99));

        SaveRecipeRequest request = new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1), "Nueva Receta", "Desc", 20, 2,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.ONE, "Salt", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "Test step")));

        createUseCase.execute(request);

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        assertEquals(OWNER, captor.getValue().getAuthorId());
    }

    @Test
    @DisplayName("update: allows the owner to modify their own recipe")
    void update_allowsOwner() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OWNER));
        when(categoryRepository.findById(new CategoryId(1)))
                .thenReturn(Optional.of(new Category(new CategoryId(1), "Appetizers")));
        when(difficultyRepository.findById(new DifficultyId(1)))
                .thenReturn(Optional.of(new Difficulty(new DifficultyId(1), "Easy")));

        SaveRecipeRequest request = new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1), "Updated Title", "Desc", 30, 2,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.ONE, "Salt", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "Test step")));

        assertDoesNotThrow(() -> updateUseCase.execute(RECIPE_ID, request));
        // Recipe is immutable; the use case persists a NEW instance derived from
        // ownerRecipe via withers + syncs. Match by identity (RecipeId), not reference.
        verify(recipeRepository).update(argThat(r -> RECIPE_ID.equals(r.getId())));
    }

    @Test
    @DisplayName("update: throws UnauthorizedRecipeAccessException for a non-owner user")
    void update_rejectsIntruder() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(INTRUDER));

        SaveRecipeRequest request = new SaveRecipeRequest(
                new CategoryId(1), new DifficultyId(1), "Title", "Desc", 30, 2,
                List.of(new SaveRecipeRequest.IngredientRequest(
                        new IngredientId(1), new UnitId(1), BigDecimal.ONE, "Salt", "g")),
                List.of(new SaveRecipeRequest.StepRequest(1, "Test step")));

        assertThrows(UnauthorizedRecipeAccessException.class,
                () -> updateUseCase.execute(RECIPE_ID, request));
        verify(recipeRepository, never()).update(any());
    }

    @Test
    @DisplayName("delete: allows the owner to delete their own recipe and wipes any favorites pinned to it")
    void delete_allowsOwner() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OWNER));

        assertDoesNotThrow(() -> deleteUseCase.execute(RECIPE_ID));

        // Atomic cross-module cleanup: favourites must be wiped BEFORE the recipe row
        // is removed so any FK firing mid-transaction sees a consistent state.
        var order = inOrder(favoriteRepository, recipeRepository);
        order.verify(favoriteRepository).deleteAllByRecipeId(RECIPE_ID);
        order.verify(recipeRepository).delete(RECIPE_ID);
    }

    @Test
    @DisplayName("delete: throws UnauthorizedRecipeAccessException for a non-owner user and leaves favorites untouched")
    void delete_rejectsIntruder() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(INTRUDER));

        assertThrows(UnauthorizedRecipeAccessException.class,
                () -> deleteUseCase.execute(RECIPE_ID));
        verify(recipeRepository, never()).delete(any(RecipeId.class));
        verify(favoriteRepository, never()).deleteAllByRecipeId(any());
    }
}
