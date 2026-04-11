package com.recetea.core.usecases;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.IRecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test: Valida la extracción del catálogo aislando la base de datos.
 */
class GetAllRecipesUseCaseTest {

    private IRecipeRepository mockRepository;
    private GetAllRecipesUseCase useCase;

    @BeforeEach
    void setUp() {
        // 1. Creamos el simulador de la base de datos
        mockRepository = Mockito.mock(IRecipeRepository.class);
        // 2. Inyectamos el simulador en nuestro caso de uso real
        useCase = new GetAllRecipesUseCase(mockRepository);
    }

    @Test
    void execute_ShouldReturnListOfRecipes_WhenRepositoryHasData() {
        // --- GIVEN (Preparación) ---
        // Creamos dos recetas falsas en la memoria RAM
        Recipe recipe1 = new Recipe(1, 1, 1, "Receta Falsa 1", "Desc", 10, 2);
        recipe1.setId(100);
        Recipe recipe2 = new Recipe(2, 2, 2, "Receta Falsa 2", "Desc", 20, 4);
        recipe2.setId(101);

        List<Recipe> fakeDatabase = Arrays.asList(recipe1, recipe2);

        // Le enseñamos al Mockito cómo debe comportarse:
        // "Cuando el caso de uso te pida findAll(), devuélvele esta lista falsa"
        when(mockRepository.findAll()).thenReturn(fakeDatabase);

        // --- WHEN (Ejecución) ---
        List<Recipe> result = useCase.execute();

        // --- THEN (Validación) ---
        // Comprobamos que el caso de uso se comunicó con el repositorio y devolvió lo correcto
        assertNotNull(result, "La lista no debe ser nula");
        assertEquals(2, result.size(), "Debe devolver exactamente 2 recetas");
        assertEquals("Receta Falsa 1", result.get(0).getTitle());
        assertEquals("Receta Falsa 2", result.get(1).getTitle());

        // Verificamos que el repositorio fue llamado exactamente 1 vez
        verify(mockRepository, times(1)).findAll();
    }
}