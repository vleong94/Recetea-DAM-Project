package com.recetea.core.usecases;

import com.recetea.core.ports.out.IRecipeRepository;
import com.recetea.core.usecases.recipe.DeleteRecipeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 * Unit Test: Valida que la orden de borrado se transmite correctamente al puerto de salida.
 */
class DeleteRecipeUseCaseTest {

    private IRecipeRepository mockRepository;
    private DeleteRecipeUseCase useCase;

    @BeforeEach
    void setUp() {
        // Mocking: Simulamos el adaptador de infraestructura
        mockRepository = Mockito.mock(IRecipeRepository.class);
        useCase = new DeleteRecipeUseCase(mockRepository);
    }

    @Test
    void execute_ShouldInvokeRepositoryDelete_WhenCalled() {
        // --- GIVEN ---
        int recipeIdToDelete = 5;

        // --- WHEN ---
        useCase.execute(recipeIdToDelete);

        // --- THEN ---
        // Verificamos que el repositorio recibió la orden exacta para ese ID
        verify(mockRepository, times(1)).delete(recipeIdToDelete);
    }
}