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
@DisplayName("AttachMediaUseCase — Almacenamiento, integridad transaccional y seguridad")
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
        useCase = new AttachMediaUseCase(recipeRepository, storageService, transactionManager, sessionService);

        lenient().when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
    }

    private Recipe buildRecipe() {
        Recipe recipe = new Recipe(
                AUTHOR_ID,
                new Category(new CategoryId(1), "Postres"),
                new Difficulty(new DifficultyId(1), "Fácil"),
                "Receta de prueba", "Descripción",
                new PreparationTime(20), new Servings(2));
        recipe.setId(RECIPE_ID);
        return recipe;
    }

    @Test
    @DisplayName("execute: almacena el archivo, añade media al agregado y persiste dentro de transacción")
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
    @DisplayName("execute: el primer media adjunto es promovido a isMain=true por el agregado")
    void execute_ShouldPromoteFirstMediaToMain() {
        when(storageService.store(any(), any())).thenReturn(STORED);
        Recipe recipe = buildRecipe();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(recipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(AUTHOR_ID));

        useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1}), "primera.jpg");

        assertTrue(recipe.getMediaItems().get(0).isMain(), "El primer media debe ser promovido a isMain=true");
    }

    @Test
    @DisplayName("execute: si el DB falla, compensa eliminando el archivo ya almacenado")
    void execute_ShouldCompensateByDeletingFile_WhenTransactionFails() {
        when(storageService.store(any(), any())).thenReturn(STORED);
        when(transactionManager.execute(any(Supplier.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1}), "foto.jpg"));

        verify(storageService).delete(STORED.storageKey());
    }

    @Test
    @DisplayName("execute: si el storage falla, el repositorio no es invocado")
    void execute_ShouldNotTouchDB_WhenStorageFails() {
        when(storageService.store(any(), any())).thenThrow(new RuntimeException("disco lleno"));

        assertThrows(RuntimeException.class, () -> useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1}), "foto.jpg"));

        verify(recipeRepository, never()).update(any());
        verify(transactionManager, never()).execute(any(Supplier.class));
    }

    @Test
    @DisplayName("execute: lanza AuthenticationRequiredException si no hay sesión activa")
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
    @DisplayName("execute: lanza UnauthorizedRecipeAccessException si el usuario no es el autor")
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
    @DisplayName("execute: lanza IllegalArgumentException si la receta no existe")
    void execute_ShouldThrow_WhenRecipeNotFound() {
        when(storageService.store(any(), any())).thenReturn(STORED);
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(RECIPE_ID, new ByteArrayInputStream(new byte[]{1}), "foto.jpg"));

        verify(recipeRepository, never()).update(any());
        verify(storageService).delete(STORED.storageKey());
    }
}
