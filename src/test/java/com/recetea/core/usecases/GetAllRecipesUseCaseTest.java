package com.recetea.core.usecases;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.out.IRecipeRepository;
import com.recetea.core.usecases.recipe.GetAllRecipesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test: Valida la extracción del catálogo aislando la base de datos.
 * Actualizado para reflejar la hidratación de ingredientes con nombres (UX).
 */
class GetAllRecipesUseCaseTest {

    private IRecipeRepository mockRepository;
    private GetAllRecipesUseCase useCase;

    @BeforeEach
    void setUp() {
        mockRepository = Mockito.mock(IRecipeRepository.class);
        useCase = new GetAllRecipesUseCase(mockRepository);
    }

    @Test
    void execute_ShouldReturnListOfRecipes_WhenRepositoryHasData() {
        // --- 1. GIVEN (Preparación) ---
        Recipe recipe1 = new Recipe(1, 1, 1, "Receta Falsa 1", "Desc", 10, 2);
        recipe1.setId(100);

        // Añadimos un ingrediente con el nuevo contrato de 5 parámetros
        recipe1.addIngredient(new RecipeIngredient(
                1, 1, new BigDecimal("500.00"), "Harina", "g"
        ));

        Recipe recipe2 = new Recipe(2, 2, 2, "Receta Falsa 2", "Desc", 20, 4);
        recipe2.setId(101);

        List<Recipe> fakeDatabase = Arrays.asList(recipe1, recipe2);

        // Definimos el comportamiento del Mock
        when(mockRepository.findAll()).thenReturn(fakeDatabase);

        // --- 2. WHEN (Ejecución) ---
        List<Recipe> result = useCase.execute();

        // --- 3. THEN (Validación) ---
        assertNotNull(result, "La lista no debe ser nula.");
        assertEquals(2, result.size(), "Debe devolver exactamente 2 recetas.");

        // Verificación de integridad de nombres en el primer elemento
        assertEquals("Receta Falsa 1", result.get(0).getTitle());
        assertFalse(result.get(0).getIngredients().isEmpty(), "La receta 1 debe tener ingredientes.");
        assertEquals("Harina", result.get(0).getIngredients().get(0).getIngredientName());

        verify(mockRepository, times(1)).findAll();
    }
}