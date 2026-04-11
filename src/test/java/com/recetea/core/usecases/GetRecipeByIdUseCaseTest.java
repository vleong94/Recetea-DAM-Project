package com.recetea.core.usecases;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.IRecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test: Valida la extracción de una receta específica y su hidratación.
 */
class GetRecipeByIdUseCaseTest {

    private IRecipeRepository mockRepository;
    private GetRecipeByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        // 1. Mocking: Simulamos la base de datos
        mockRepository = Mockito.mock(IRecipeRepository.class);
        // 2. Wiring: Inyectamos el simulador en el Caso de Uso real
        useCase = new GetRecipeByIdUseCase(mockRepository);
    }

    @Test
    void execute_ShouldReturnHydratedRecipe_WhenRecipeExists() {
        // --- GIVEN (Preparación del estado) ---
        int targetId = 1;
        // Creamos una receta falsa en memoria RAM
        Recipe fakeRecipe = new Recipe(1, 1, 1, "Receta Completa", "Desc", 45, 4);
        fakeRecipe.setId(targetId);
        // La hidratamos con un par de ingredientes falsos
        fakeRecipe.addIngredient(new RecipeIngredient(10, 1, 250.0));
        fakeRecipe.addIngredient(new RecipeIngredient(20, 2, 1.5));

        // Entrenamos al Mock: "Cuando te pidan el ID 1, devuelve esta entidad profunda"
        when(mockRepository.findById(targetId)).thenReturn(Optional.of(fakeRecipe));

        // --- WHEN (Ejecución del contrato) ---
        Optional<Recipe> result = useCase.execute(targetId);

        // --- THEN (Validación estricta) ---
        assertTrue(result.isPresent(), "El Optional NO debe estar vacío.");
        assertEquals(targetId, result.get().getId(), "El ID extraído debe coincidir.");
        assertEquals(2, result.get().getIngredients().size(), "El Aggregate Root debe contener exactamente 2 ingredientes.");

        // Verificamos que el puerto de salida fue invocado correctamente
        verify(mockRepository, times(1)).findById(targetId);
    }

    @Test
    void execute_ShouldReturnEmptyOptional_WhenRecipeDoesNotExist() {
        // --- GIVEN ---
        int missingId = 999;
        // Entrenamos al Mock para simular un fallo de búsqueda en SQL
        when(mockRepository.findById(missingId)).thenReturn(Optional.empty());

        // --- WHEN ---
        Optional<Recipe> result = useCase.execute(missingId);

        // --- THEN ---
        assertFalse(result.isPresent(), "El Optional DEBE estar vacío si no existe el ID.");
        verify(mockRepository, times(1)).findById(missingId);
    }
}