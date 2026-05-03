package com.recetea.core.recipe.application.usecases.media;

import com.recetea.core.recipe.application.ports.out.media.IMediaStorageService;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RemoveMediaUseCase — Coordinated removal from the DB and physical storage")
class RemoveMediaUseCaseTest {

    @Mock private IRecipeRepository recipeRepository;
    @Mock private IMediaStorageService storageService;
    @Mock private ITransactionManager transactionManager;
    @Mock private IUserSessionService sessionService;

    private RemoveMediaUseCase useCase;

    private static final UserId AUTHOR_ID  = new UserId(1);
    private static final UserId OTHER_ID   = new UserId(2);
    private static final RecipeId RECIPE_ID   = new RecipeId(10);
    private static final RecipeMediaId MEDIA_ID = new RecipeMediaId(5);
    private static final String STORAGE_KEY = "media/foto.jpg";

    @BeforeEach
    void setUp() {
        useCase = new RemoveMediaUseCase(recipeRepository, storageService, transactionManager, sessionService);

        lenient().when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
    }

    private Recipe buildRecipeWithMedia() {
        RecipeMedia media = new RecipeMedia(MEDIA_ID, RECIPE_ID, STORAGE_KEY,
                "LOCAL", "image/jpeg", 2048L, true, 0);
        return new Recipe(
                RECIPE_ID, AUTHOR_ID,
                new Category(new CategoryId(1), "Desserts"),
                new Difficulty(new DifficultyId(1), "Easy"),
                "Test recipe", "Description",
                new PreparationTime(20), new Servings(2),
                java.math.BigDecimal.ZERO, 0)
                .withMediaItems(java.util.List.of(media));
    }

    @Test
    @DisplayName("execute: removes from the aggregate, persists in DB, and then deletes the physical file")
    void execute_ShouldRemoveFromAggregateAndDeleteFile_OnHappyPath() {
        Recipe recipe = buildRecipeWithMedia();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(recipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(AUTHOR_ID));

        useCase.execute(RECIPE_ID, MEDIA_ID);

        verify(recipeRepository).update(argThat(r -> r.getMediaItems().isEmpty()));
        verify(storageService).delete(STORAGE_KEY);
        // DB update must happen inside the transaction, file deletion after.
        var order = inOrder(transactionManager, storageService);
        order.verify(transactionManager).execute(any(Supplier.class));
        order.verify(storageService).delete(STORAGE_KEY);
    }

    @Test
    @DisplayName("execute: does not delete the physical file when the DB transaction fails")
    void execute_ShouldNotDeleteFile_WhenTransactionFails() {
        when(transactionManager.execute(any(Supplier.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> useCase.execute(RECIPE_ID, MEDIA_ID));

        verify(storageService, never()).delete(any());
    }

    @Test
    @DisplayName("execute: throws AuthenticationRequiredException when there is no active session")
    void execute_ShouldThrow_WhenNoSession() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(buildRecipeWithMedia()));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthenticationRequiredException.class,
                () -> useCase.execute(RECIPE_ID, MEDIA_ID));

        verify(recipeRepository, never()).update(any());
        verify(storageService, never()).delete(any());
    }

    @Test
    @DisplayName("execute: throws UnauthorizedRecipeAccessException when the user is not the author")
    void execute_ShouldThrow_WhenNotOwner() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(buildRecipeWithMedia()));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OTHER_ID));

        assertThrows(UnauthorizedRecipeAccessException.class,
                () -> useCase.execute(RECIPE_ID, MEDIA_ID));

        verify(recipeRepository, never()).update(any());
        verify(storageService, never()).delete(any());
    }

    @Test
    @DisplayName("execute: throws IllegalArgumentException when the recipe does not exist")
    void execute_ShouldThrow_WhenRecipeNotFound() {
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(RECIPE_ID, MEDIA_ID));

        verify(recipeRepository, never()).update(any());
        verify(storageService, never()).delete(any());
    }

    @Test
    @DisplayName("execute: throws IllegalArgumentException when the mediaId does not exist in the aggregate")
    void execute_ShouldThrow_WhenMediaIdNotFound() {
        Recipe recipe = buildRecipeWithMedia();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(recipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(AUTHOR_ID));

        RecipeMediaId unknownId = new RecipeMediaId(999);
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(RECIPE_ID, unknownId));

        verify(recipeRepository, never()).update(any());
        verify(storageService, never()).delete(any());
    }
}
