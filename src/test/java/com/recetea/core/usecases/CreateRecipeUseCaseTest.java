package com.recetea.core.usecases;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.out.IRecipeRepository;
import com.recetea.core.ports.in.dto.CreateRecipeCommand;
import com.recetea.core.usecases.recipe.CreateRecipeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test: Valida la lógica de negocio de la creación de recetas.
 * Actualizado para verificar el flujo de nombres descriptivos (UX).
 */
class CreateRecipeUseCaseTest {

    private IRecipeRepository mockRepository;
    private CreateRecipeUseCase useCase;

    @BeforeEach
    void setUp() {
        mockRepository = Mockito.mock(IRecipeRepository.class);
        useCase = new CreateRecipeUseCase(mockRepository);
    }

    @Test
    void execute_ShouldCreateRecipeAndReturnId_WhenCommandIsValid() {
        // --- 1. GIVEN ---
        BigDecimal expectedQuantity = new BigDecimal("200.00");

        // Actualizamos el comando con los 5 parámetros requeridos en IngredientCommand
        CreateRecipeCommand command = new CreateRecipeCommand(
                1, 2, 3,
                "Tarta de Prueba",
                "Descripción test",
                30, 4,
                List.of(new CreateRecipeCommand.IngredientCommand(
                        1, 1, expectedQuantity, "Harina de Trigo", "g"
                ))
        );

        doAnswer(invocation -> {
            Recipe recipeArgument = invocation.getArgument(0);
            recipeArgument.setId(99);
            return null;
        }).when(mockRepository).save(any(Recipe.class));

        // --- 2. WHEN ---
        int returnedId = useCase.execute(command);

        // --- 3. THEN ---
        assertEquals(99, returnedId, "El caso de uso debe devolver el ID generado por el repositorio.");

        ArgumentCaptor<Recipe> recipeCaptor = ArgumentCaptor.forClass(Recipe.class);
        verify(mockRepository, times(1)).save(recipeCaptor.capture());

        Recipe savedRecipe = recipeCaptor.getValue();

        assertEquals("Tarta de Prueba", savedRecipe.getTitle());
        assertEquals(30, savedRecipe.getPreparationTimeMinutes());
        assertEquals(1, savedRecipe.getIngredients().size(), "Debe haber 1 ingrediente mapeado.");

        // Verificamos que los nuevos campos de nombre se han mapeado correctamente
        assertEquals("Harina de Trigo", savedRecipe.getIngredients().get(0).getIngredientName());
        assertEquals("g", savedRecipe.getIngredients().get(0).getUnitName());

        // Verificación de precisión matemática
        BigDecimal actualQuantity = savedRecipe.getIngredients().get(0).getQuantity();
        assertTrue(expectedQuantity.compareTo(actualQuantity) == 0,
                "La cantidad mapeada (" + actualQuantity + ") debe ser " + expectedQuantity);
    }

    @Test
    void execute_ShouldThrowException_WhenRepositoryFailsToAssignId() {
        // --- GIVEN ---
        // Usamos lista vacía, por lo que no hace falta definir ingredientes aquí
        CreateRecipeCommand command = new CreateRecipeCommand(
                1, 1, 1, "Fallo", "", 10, 2, Collections.emptyList());

        doNothing().when(mockRepository).save(any(Recipe.class));

        // --- WHEN & THEN ---
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            useCase.execute(command);
        });

        assertTrue(exception.getMessage().contains("identidad válida"),
                "Debe lanzar excepción si el ID autogenerado no se asigna.");
    }
}