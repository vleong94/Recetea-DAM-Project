package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.*;
import com.recetea.core.recipe.domain.vo.*;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Seguridad de Propiedad — Create, Update y Delete")
class OwnershipSecurityTest {

    @Mock private IRecipeRepository recipeRepository;
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

    @BeforeEach
    void setUp() {
        createUseCase = new CreateRecipeUseCase(
                recipeRepository, categoryRepository, difficultyRepository, transactionManager, sessionService);
        updateUseCase = new UpdateRecipeUseCase(
                recipeRepository, categoryRepository, difficultyRepository, transactionManager, sessionService);
        deleteUseCase = new DeleteRecipeUseCase(recipeRepository, transactionManager, sessionService);

        ownerRecipe = new Recipe(
                OWNER,
                new Category(new CategoryId(1), "Entrantes"),
                new Difficulty(new DifficultyId(1), "Fácil"),
                "Receta del Propietario", "Descripción",
                new PreparationTime(20), new Servings(2));
        ownerRecipe.setId(new RecipeId(10));

        doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; })
                .when(transactionManager).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("create: la receta se asigna al usuario de la sesión activa, no al payload")
    void create_assignsAuthorFromSession() {
        when(sessionService.getCurrentUserId()).thenReturn(OWNER);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(new Category(new CategoryId(1), "Entrantes")));
        when(difficultyRepository.findById(1)).thenReturn(Optional.of(new Difficulty(new DifficultyId(1), "Fácil")));
        when(transactionManager.execute(any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, java.util.function.Supplier.class).get());
        doAnswer(inv -> { ((Recipe) inv.getArgument(0)).setId(new RecipeId(99)); return null; })
                .when(recipeRepository).save(any(Recipe.class));

        SaveRecipeRequest request = new SaveRecipeRequest(1, 1, "Nueva Receta", "Desc", 20, 2, List.of(), List.of());

        createUseCase.execute(request);

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        assertEquals(OWNER, captor.getValue().getAuthorId());
    }

    @Test
    @DisplayName("update: el propietario puede modificar su propia receta")
    void update_allowsOwner() {
        when(recipeRepository.findById(10)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(OWNER);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(new Category(new CategoryId(1), "Entrantes")));
        when(difficultyRepository.findById(1)).thenReturn(Optional.of(new Difficulty(new DifficultyId(1), "Fácil")));

        SaveRecipeRequest request = new SaveRecipeRequest(1, 1, "Título Actualizado", "Desc", 30, 2, List.of(), List.of());

        assertDoesNotThrow(() -> updateUseCase.execute(10, request));
        verify(recipeRepository).update(ownerRecipe);
    }

    @Test
    @DisplayName("update: un usuario ajeno lanza UnauthorizedRecipeAccessException")
    void update_rejectsIntruder() {
        when(recipeRepository.findById(10)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(INTRUDER);

        SaveRecipeRequest request = new SaveRecipeRequest(1, 1, "Título", "Desc", 30, 2, List.of(), List.of());

        assertThrows(UnauthorizedRecipeAccessException.class,
                () -> updateUseCase.execute(10, request));
        verify(recipeRepository, never()).update(any());
    }

    @Test
    @DisplayName("delete: el propietario puede eliminar su propia receta")
    void delete_allowsOwner() {
        when(recipeRepository.findById(10)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(OWNER);

        assertDoesNotThrow(() -> deleteUseCase.execute(10));
        verify(recipeRepository).delete(10);
    }

    @Test
    @DisplayName("delete: un usuario ajeno lanza UnauthorizedRecipeAccessException")
    void delete_rejectsIntruder() {
        when(recipeRepository.findById(10)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(INTRUDER);

        assertThrows(UnauthorizedRecipeAccessException.class,
                () -> deleteUseCase.execute(10));
        verify(recipeRepository, never()).delete(anyInt());
    }
}
