package com.recetea.core.recipe.application.usecases.media;

import com.recetea.core.recipe.application.ports.out.media.IMediaStorageService;
import com.recetea.core.recipe.application.ports.out.media.StorageResult;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.shared.application.ConcurrencyGuard;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachMediaUseCase — File storage, transactional integrity, and security")
class AttachMediaUseCaseTest {

    @Mock private IRecipeRepository recipeRepository;
    @Mock private IMediaStorageService storageService;
    @Mock private ITransactionManager transactionManager;
    @Mock private IUserSessionService sessionService;

    private AttachMediaUseCase useCase;

    private static final UserId AUTHOR_ID = new UserId(1);
    private static final UserId OTHER_ID  = new UserId(2);
    private static final RecipeId RECIPE_ID = new RecipeId(10);
    private static final StorageResult STORED = new StorageResult("media/abc.jpg", 1024L, "image/jpeg");

    @BeforeEach
    void setUp() {
        useCase = new AttachMediaUseCase(recipeRepository, storageService, transactionManager, sessionService,
                new ConcurrencyGuard(Integer.MAX_VALUE));

        lenient().when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
    }

    private Recipe buildRecipe() {
        return new Recipe(
                RECIPE_ID, AUTHOR_ID,
                new Category(new CategoryId(1), "Desserts"),
                new Difficulty(new DifficultyId(1), "Easy"),
                "Test recipe", "Description",
                new PreparationTime(20), new Servings(2),
                java.math.BigDecimal.ZERO, 0);
    }

    @Test
    @DisplayName("execute: stores the file, adds media to the aggregate and persists within a transaction")
    void execute_ShouldStoreAndPersistMedia_OnHappyPath() {
        when(storageService.store(any(), any())).thenReturn(STORED);
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(buildRecipe()));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(AUTHOR_ID));

        useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1, 2}), "foto.jpg");

        verify(storageService).store(any(), eq("foto.jpg"));
        verify(recipeRepository).update(argThat(r -> !r.getMediaItems().isEmpty()));
        verify(transactionManager).execute(any(Supplier.class));
    }

    @Test
    @DisplayName("execute: the first attached media is promoted to isMain=true by the aggregate")
    void execute_ShouldPromoteFirstMediaToMain() {
        when(storageService.store(any(), any())).thenReturn(STORED);
        Recipe recipe = buildRecipe();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(recipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(AUTHOR_ID));

        useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1}), "primera.jpg");

        // Capture the post-addMedia aggregate (Recipe is immutable; addMedia returns a new instance).
        org.mockito.ArgumentCaptor<Recipe> captor = org.mockito.ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).update(captor.capture());
        assertTrue(captor.getValue().getMediaItems().get(0).isMain(),
                "The first media item must be promoted to isMain=true");
    }

    @Test
    @DisplayName("execute: compensates by deleting the already-stored file when the transaction fails")
    void execute_ShouldCompensateByDeletingFile_WhenTransactionFails() {
        when(storageService.store(any(), any())).thenReturn(STORED);
        when(transactionManager.execute(any(Supplier.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1}), "foto.jpg"));

        verify(storageService).delete(STORED.storageKey());
    }

    @Test
    @DisplayName("execute: does not touch the DB when storage fails")
    void execute_ShouldNotTouchDB_WhenStorageFails() {
        when(storageService.store(any(), any())).thenThrow(new RuntimeException("disk full"));

        assertThrows(RuntimeException.class, () -> useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1}), "foto.jpg"));

        verify(recipeRepository, never()).update(any());
        verify(transactionManager, never()).execute(any(Supplier.class));
    }

    @Test
    @DisplayName("execute: throws AuthenticationRequiredException when there is no active session")
    void execute_ShouldThrow_WhenNoSession() {
        when(storageService.store(any(), any())).thenReturn(STORED);
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(buildRecipe()));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthenticationRequiredException.class,
                () -> useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1}), "foto.jpg"));

        verify(recipeRepository, never()).update(any());
        verify(storageService).delete(STORED.storageKey());
    }

    @Test
    @DisplayName("execute: throws UnauthorizedRecipeAccessException when the user is not the author")
    void execute_ShouldThrow_WhenNotOwner() {
        when(storageService.store(any(), any())).thenReturn(STORED);
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(buildRecipe()));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OTHER_ID));

        assertThrows(UnauthorizedRecipeAccessException.class,
                () -> useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1}), "foto.jpg"));

        verify(recipeRepository, never()).update(any());
        verify(storageService).delete(STORED.storageKey());
    }

    @Test
    @DisplayName("execute: throws InvalidRecipeDataException when the recipe does not exist")
    void execute_ShouldThrow_WhenRecipeNotFound() {
        when(storageService.store(any(), any())).thenReturn(STORED);
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.empty());

        assertThrows(com.recetea.core.recipe.domain.InvalidRecipeDataException.class,
                () -> useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1}), "foto.jpg"));

        verify(recipeRepository, never()).update(any());
        verify(storageService).delete(STORED.storageKey());
    }

    @Test
    @DisplayName("execute: throws InvalidRecipeDataException carrying every validation failure when inputs are missing")
    void execute_ShouldAccumulateValidationErrors_WhenInputsAreNull() {
        com.recetea.core.recipe.domain.InvalidRecipeDataException ex = assertThrows(
                com.recetea.core.recipe.domain.InvalidRecipeDataException.class,
                () -> useCase.execute(null, null, " "));

        // ValidationResult is non-short-circuit: all three input failures are reported.
        java.util.List<String> errors = ex.errors();
        assertEquals(3, errors.size(), "All three input validations should accumulate; got: " + errors);
        verify(storageService, never()).store(any(), any());
        verify(recipeRepository, never()).update(any());
    }
}
