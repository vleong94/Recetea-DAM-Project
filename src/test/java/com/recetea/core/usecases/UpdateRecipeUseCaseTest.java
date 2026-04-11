package com.recetea.core.usecases;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.IRecipeRepository;
import com.recetea.core.ports.in.CreateRecipeCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        // --- GIVEN (Escenario) ---
        int recipeId = 10;
        CreateRecipeCommand command = new CreateRecipeCommand(
                1, 1, 1, "Título Editado", "Descripción Nueva", 30, 2,
                List.of(new CreateRecipeCommand.IngredientCommand(5, 1, 100.0))
        );

        // --- WHEN (Acción) ---
        useCase.execute(recipeId, command);

        // --- THEN (Verificación) ---
        // Usamos un ArgumentCaptor para interceptar el objeto que se envía al repositorio
        ArgumentCaptor<Recipe> recipeCaptor = ArgumentCaptor.forClass(Recipe.class);
        verify(mockRepository, times(1)).update(recipeCaptor.capture());

        Recipe capturedRecipe = recipeCaptor.getValue();

        // Validamos que el mapeo fue perfecto
        assertEquals(recipeId, capturedRecipe.getId(), "El ID debe ser el que pasamos por parámetro");
        assertEquals("Título Editado", capturedRecipe.getTitle());
        assertEquals(1, capturedRecipe.getIngredients().size(), "Debe haber mapeado el ingrediente del command");
        assertEquals(100.0, capturedRecipe.getIngredients().get(0).getQuantity());
    }
}