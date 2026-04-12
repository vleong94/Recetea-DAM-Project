package com.recetea.core.usecases;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.out.IRecipeRepository;
import com.recetea.core.usecases.recipe.GetRecipeByIdUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test: Valida la extracción de una receta específica y su hidratación.
 * Sincronizado con el constructor de 5 parámetros de RecipeIngredient.
 */
class GetRecipeByIdUseCaseTest {

    private IRecipeRepository mockRepository;
    private GetRecipeByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        mockRepository = Mockito.mock(IRecipeRepository.class);
        useCase = new GetRecipeByIdUseCase(mockRepository);
    }

    @Test
    void execute_ShouldReturnHydratedRecipe_WhenRecipeExists() {
        // --- 1. GIVEN ---
        int targetId = 1;
        Recipe fakeRecipe = new Recipe(1, 1, 1, "Receta Completa", "Desc", 45, 4);
        fakeRecipe.setId(targetId);

        // CORRECCIÓN: Inyectamos los 5 parámetros requeridos (incluye nombres para UX)
        fakeRecipe.addIngredient(new RecipeIngredient(
                10, 1, new BigDecimal("250.00"), "Pechuga de Pollo", "g"
        ));
        fakeRecipe.addIngredient(new RecipeIngredient(
                20, 2, new BigDecimal("1.50"), "Aceite de Oliva", "ml"
        ));

        when(mockRepository.findById(targetId)).thenReturn(Optional.of(fakeRecipe));

        // --- 2. WHEN ---
        Optional<Recipe> result = useCase.execute(targetId);

        // --- 3. THEN ---
        assertTrue(result.isPresent(), "El Optional NO debe estar vacío.");
        assertEquals(targetId, result.get().getId(), "El ID extraído debe coincidir.");

        // Verificación de los nuevos campos de nombre
        assertEquals("Pechuga de Pollo", result.get().getIngredients().get(0).getIngredientName());
        assertEquals("g", result.get().getIngredients().get(0).getUnitName());

        verify(mockRepository, times(1)).findById(targetId);
    }

    @Test
    void execute_ShouldReturnEmptyOptional_WhenRecipeDoesNotExist() {
        // --- GIVEN ---
        int missingId = 999;
        when(mockRepository.findById(missingId)).thenReturn(Optional.empty());

        // --- WHEN ---
        Optional<Recipe> result = useCase.execute(missingId);

        // --- THEN ---
        assertFalse(result.isPresent(), "El Optional DEBE estar vacío si no existe el ID.");
        verify(mockRepository, times(1)).findById(missingId);
    }
}