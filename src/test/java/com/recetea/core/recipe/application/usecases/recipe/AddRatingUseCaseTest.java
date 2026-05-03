package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.AddRatingRequest;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.RecipeNotFoundException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.shared.application.ConcurrencyGuard;
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

import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddRatingUseCase — Transactional integrity and invariant enforcement")
class AddRatingUseCaseTest {

    @Mock private IRecipeRepository recipeRepository;
    @Mock private IUserSessionService sessionService;
    @Mock private ITransactionManager transactionManager;

    private AddRatingUseCase useCase;

    private static final UserId  AUTHOR_ID  = new UserId(1);
    private static final UserId  VOTER_ID   = new UserId(2);
    private static final RecipeId RECIPE_ID = new RecipeId(10);

    @BeforeEach
    void setUp() {
        useCase = new AddRatingUseCase(recipeRepository, transactionManager, sessionService,
                new ConcurrencyGuard(Integer.MAX_VALUE));

        when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
    }

    private Recipe buildRecipe() {
        return new Recipe(
                RECIPE_ID, AUTHOR_ID,
                new Category(new CategoryId(1), "Desserts"),
                new Difficulty(new DifficultyId(1), "Easy"),
                "Test Recipe", "Description",
                new PreparationTime(20), new Servings(2),
                java.math.BigDecimal.ZERO, 0);
    }

    @Test
    @DisplayName("execute: happy path — delegates to the aggregate and persists within a transaction")
    void execute_ShouldSucceed_WhenValidDataProvided() {
        Recipe recipe = buildRecipe();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(recipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(VOTER_ID));

        AddRatingRequest request = new AddRatingRequest(RECIPE_ID, new Score(5), "Excellent recipe");

        useCase.execute(request);

        // Recipe is an immutable record: addRating returns a new aggregate carrying the
        // rating. The original `recipe` mock-stub return is unchanged. Capture the
        // instance handed to update() to inspect post-rating state.
        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository, times(1)).update(captor.capture());
        Recipe persisted = captor.getValue();
        assertEquals(1, persisted.getRatings().size(), "The persisted aggregate must contain the added rating");
        assertEquals(VOTER_ID, persisted.getRatings().get(0).userId());
        assertEquals(5, persisted.getRatings().get(0).score().value());

        verify(recipeRepository, never()).updateSocialMetrics(any(), any(), anyInt());

        // Transaction boundary must have been entered
        verify(transactionManager, times(1)).execute(any(Supplier.class));
    }

    @Test
    @DisplayName("execute: throws RecipeNotFoundException and does not persist when the recipe does not exist")
    void execute_ShouldThrowException_WhenRecipeDoesNotExist() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.empty());

        AddRatingRequest request = new AddRatingRequest(RECIPE_ID, new Score(4), "Comment");

        assertThrows(RecipeNotFoundException.class, () -> useCase.execute(request),
                "Must throw RecipeNotFoundException when the recipe does not exist");

        verify(recipeRepository, never()).update(any());
        verify(recipeRepository, never()).updateSocialMetrics(any(), any(), anyInt());
    }

    @Test
    @DisplayName("execute: throws AuthenticationRequiredException when no user is in session")
    void execute_ShouldThrowAuthenticationRequiredException_WhenSessionIsEmpty() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(buildRecipe()));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.empty());

        AddRatingRequest request = new AddRatingRequest(RECIPE_ID, new Score(5), "Comment");

        assertThrows(AuthenticationRequiredException.class, () -> useCase.execute(request),
                "Must throw AuthenticationRequiredException when the session is empty");

        verify(recipeRepository, never()).update(any());
        verify(recipeRepository, never()).updateSocialMetrics(any(), any(), anyInt());
    }
}
