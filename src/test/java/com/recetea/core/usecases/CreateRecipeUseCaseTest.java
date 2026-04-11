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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test: Valida la lógica de negocio de la creación de recetas
 * aislando completamente la base de datos mediante Mockito.
 */
class CreateRecipeUseCaseTest {

    private IRecipeRepository mockRepository;
    private CreateRecipeUseCase useCase;

    @BeforeEach
    void setUp() {
        // Inicializamos el Mock (un repositorio falso) antes de cada test
        mockRepository = Mockito.mock(IRecipeRepository.class);
        // Inyectamos el Mock en el caso de uso real
        useCase = new CreateRecipeUseCase(mockRepository);
    }

    @Test
    void execute_ShouldCreateRecipeAndReturnId_WhenCommandIsValid() {
        // --- 1. GIVEN (Preparación del escenario) ---
        CreateRecipeCommand command = new CreateRecipeCommand(
                1, 2, 3,
                "Tarta de Prueba",
                "Descripción test",
                30, 4,
                List.of(new CreateRecipeCommand.IngredientCommand(1, 1, 200.0))
        );

        // Simulamos qué debe hacer el mock cuando el UseCase le llame al método 'save'.
        // Le decimos: "Cuando te pasen un objeto Recipe, asígnale el ID 99 mágicamente".
        doAnswer(invocation -> {
            Recipe recipeArgument = invocation.getArgument(0);
            recipeArgument.setId(99);
            return null; // El método save() es void
        }).when(mockRepository).save(any(Recipe.class));

        // --- 2. WHEN (Ejecución de la acción) ---
        int returnedId = useCase.execute(command);

        // --- 3. THEN (Verificación de resultados) ---

        // Comprobamos que el caso de uso devuelve el ID correcto (99)
        assertEquals(99, returnedId, "El caso de uso debe devolver el ID generado por el repositorio.");

        // ArgumentCaptor: Atrapamos el objeto Recipe exacto que el UseCase le pasó al Repositorio
        ArgumentCaptor<Recipe> recipeCaptor = ArgumentCaptor.forClass(Recipe.class);
        verify(mockRepository, times(1)).save(recipeCaptor.capture());

        Recipe savedRecipe = recipeCaptor.getValue();

        // Verificamos que el mapeo (Data Binding) del Command a la Entidad fue perfecto
        assertEquals("Tarta de Prueba", savedRecipe.getTitle());
        assertEquals(30, savedRecipe.getPreparationTimeMinutes());
        assertEquals(1, savedRecipe.getIngredients().size(), "Debe haber 1 ingrediente mapeado.");
        assertEquals(200.0, savedRecipe.getIngredients().get(0).getQuantity());
    }

    @Test
    void execute_ShouldThrowException_WhenRepositoryFailsToAssignId() {
        // --- GIVEN ---
        CreateRecipeCommand command = new CreateRecipeCommand(
                1, 1, 1, "Fallo", "", 10, 2, Collections.emptyList());

        // Simulamos un fallo catastrófico en la base de datos: el repositorio no asigna el ID.
        doNothing().when(mockRepository).save(any(Recipe.class));

        // --- WHEN & THEN ---
        // Comprobamos que el caso de uso se da cuenta del error y lanza la excepción correcta
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            useCase.execute(command);
        });

        assertTrue(exception.getMessage().contains("Fallo de integridad"));
    }
}