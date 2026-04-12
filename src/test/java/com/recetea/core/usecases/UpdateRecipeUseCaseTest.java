package com.recetea.core.usecases;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.out.IRecipeRepository;
import com.recetea.core.ports.in.dto.CreateRecipeCommand;
import com.recetea.core.usecases.recipe.UpdateRecipeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test: Valida la lógica de actualización.
 * Sincronizado con el constructor de 5 parámetros de IngredientCommand.
 */
class UpdateRecipeUseCaseTest {

    private IRecipeRepository mockRepository;
    private UpdateRecipeUseCase useCase;

    @BeforeEach
    void setUp() {
        mockRepository = Mockito.mock(IRecipeRepository.class);
        useCase = new UpdateRecipeUseCase(mockRepository);
    }

    @Test
    void execute_ShouldMapCommandToEntityAndCallUpdate() {
        // --- 1. GIVEN ---
        int recipeId = 10;
        BigDecimal expectedQuantity = new BigDecimal("100.00");

        // CORRECCIÓN: Instanciamos el comando con los 5 parámetros requeridos
        CreateRecipeCommand command = new CreateRecipeCommand(
                1, 1, 1, "Título Editado", "Descripción Nueva", 30, 2,
                List.of(new CreateRecipeCommand.IngredientCommand(
                        5, 1, expectedQuantity, "Pollo Troceado", "g"
                ))
        );

        // --- 2. WHEN ---
        useCase.execute(recipeId, command);

        // --- 3. THEN ---
        ArgumentCaptor<Recipe> recipeCaptor = ArgumentCaptor.forClass(Recipe.class);
        verify(mockRepository, times(1)).update(recipeCaptor.capture());

        Recipe capturedRecipe = recipeCaptor.getValue();

        // Validaciones de Integridad
        assertEquals(recipeId, capturedRecipe.getId(), "El ID debe ser el que pasamos por parámetro.");
        assertEquals("Título Editado", capturedRecipe.getTitle());
        assertEquals(1, capturedRecipe.getIngredients().size(), "Debe haber mapeado el ingrediente del command.");

        // Verificación de los nuevos campos de nombre (UX)
        assertEquals("Pollo Troceado", capturedRecipe.getIngredients().get(0).getIngredientName());
        assertEquals("g", capturedRecipe.getIngredients().get(0).getUnitName());

        // Verificación de precisión decimal
        BigDecimal actualQuantity = capturedRecipe.getIngredients().get(0).getQuantity();
        assertTrue(expectedQuantity.compareTo(actualQuantity) == 0,
                "La cantidad mapeada debe ser exactamente " + expectedQuantity);
    }
}