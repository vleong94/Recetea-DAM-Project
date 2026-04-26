package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.AddRatingRequest;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.RecipeNotFoundException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddRatingUseCase — Integridad transaccional y cumplimiento de invariantes")
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
        useCase = new AddRatingUseCase(recipeRepository, transactionManager, sessionService);

        when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
    }

    private Recipe buildRecipe() {
        Recipe recipe = new Recipe(
                AUTHOR_ID,
                new Category(new CategoryId(1), "Postres"),
                new Difficulty(new DifficultyId(1), "Fácil"),
                "Receta de Prueba", "Descripción",
                new PreparationTime(20),
                new Servings(2));
        recipe.setId(RECIPE_ID);
        return recipe;
    }

    @Test
    @DisplayName("execute: camino feliz — delega en el agregado y persiste dentro de transacción")
    void execute_ShouldSucceed_WhenValidDataProvided() {
        Recipe recipe = buildRecipe();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(recipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(VOTER_ID));

        AddRatingRequest request = new AddRatingRequest(RECIPE_ID, new Score(5), "Excelente receta");

        useCase.execute(request);

        // Domain invariant: rating was added to the aggregate
        assertEquals(1, recipe.getRatings().size(), "El agregado debe contener la valoración añadida");
        assertEquals(VOTER_ID, recipe.getRatings().get(0).getUserId());
        assertEquals(5, recipe.getRatings().get(0).getScore().value());

        // Repository must delegate the full update (which handles metrics via dirty flag)
        verify(recipeRepository, times(1)).update(eq(recipe));
        verify(recipeRepository, never()).updateSocialMetrics(any(), any(), anyInt());

        // Transaction boundary must have been entered
        verify(transactionManager, times(1)).execute(any(Supplier.class));
    }

    @Test
    @DisplayName("execute: lanza excepción y no persiste si la receta no existe")
    void execute_ShouldThrowException_WhenRecipeDoesNotExist() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.empty());

        AddRatingRequest request = new AddRatingRequest(RECIPE_ID, new Score(4), "Comentario");

        assertThrows(RecipeNotFoundException.class, () -> useCase.execute(request),
                "Debe lanzar RecipeNotFoundException cuando la receta no existe");

        verify(recipeRepository, never()).update(any());
        verify(recipeRepository, never()).updateSocialMetrics(any(), any(), anyInt());
    }

    @Test
    @DisplayName("execute: lanza AuthenticationRequiredException si no hay usuario en sesión")
    void execute_ShouldThrowAuthenticationRequiredException_WhenSessionIsEmpty() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(buildRecipe()));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.empty());

        AddRatingRequest request = new AddRatingRequest(RECIPE_ID, new Score(5), "Comentario");

        assertThrows(AuthenticationRequiredException.class, () -> useCase.execute(request),
                "Debe lanzar AuthenticationRequiredException cuando la sesión está vacía");

        verify(recipeRepository, never()).update(any());
        verify(recipeRepository, never()).updateSocialMetrics(any(), any(), anyInt());
    }
}
