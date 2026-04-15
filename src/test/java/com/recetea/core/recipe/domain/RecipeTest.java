package com.recetea.core.recipe.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas unitarias para el Aggregate Root Recipe.
 * Valida la integridad de los invariantes de negocio, el manejo estricto de
 * excepciones de dominio, la validación del Value Object de identidad de autor,
 * y el encapsulamiento seguro de las colecciones internas.
 */
class RecipeTest {

    @Test
    @DisplayName("Debe instanciar una receta válida correctamente")
    void shouldCreateValidRecipe() {
        // Arrange & Act
        Recipe recipe = new Recipe(new Recipe.AuthorId(1), 10, 2, "Tortilla de Patatas", "Pasos detallados", 30, 4);

        // Assert
        assertAll(
                () -> assertEquals("Tortilla de Patatas", recipe.getTitle()),
                () -> assertEquals(30, recipe.getPreparationTimeMinutes()),
                () -> assertEquals(4, recipe.getServings()),
                () -> assertTrue(recipe.getIngredients().isEmpty(), "La lista de ingredientes debe iniciar vacía.")
        );
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException si la identidad del autor es inválida")
    void shouldThrowExceptionWhenAuthorIdentityIsInvalid() {
        // Act & Assert
        // Verifica que el Value Object de identidad proteja sus propias reglas matemáticas.
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new Recipe.AuthorId(0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Recipe.AuthorId(-5))
        );
    }

    @Test
    @DisplayName("Debe lanzar RecipeValidationException si el título es inválido")
    void shouldThrowExceptionWhenTitleIsInvalid() {
        // Act & Assert
        // Verifica que el Aggregate Root rechace instanciaciones con identificadores nominales nulos o vacíos.
        assertAll(
                () -> assertThrows(Recipe.RecipeValidationException.class,
                        () -> new Recipe(new Recipe.AuthorId(1), 1, 1, null, "Desc", 10, 1)),
                () -> assertThrows(Recipe.RecipeValidationException.class,
                        () -> new Recipe(new Recipe.AuthorId(1), 1, 1, "   ", "Desc", 10, 1))
        );
    }

    @Test
    @DisplayName("Debe lanzar InvalidRecipeMetricException si las métricas numéricas son inválidas")
    void shouldThrowExceptionWhenMetricsAreInvalid() {
        // Act & Assert
        // Valida que el constructor y los mutadores impidan estados con tiempos o capacidades ilógicas.
        assertAll(
                // Fronteras del tiempo de preparación (<= 0)
                () -> assertThrows(Recipe.InvalidRecipeMetricException.class,
                        () -> new Recipe(new Recipe.AuthorId(1), 1, 1, "Sopa", "Desc", 0, 4)),
                () -> assertThrows(Recipe.InvalidRecipeMetricException.class,
                        () -> new Recipe(new Recipe.AuthorId(1), 1, 1, "Sopa", "Desc", -15, 4)),
                // Fronteras del rendimiento en raciones (<= 0)
                () -> assertThrows(Recipe.InvalidRecipeMetricException.class,
                        () -> new Recipe(new Recipe.AuthorId(1), 1, 1, "Sopa", "Desc", 20, 0)),
                // Mutación post-instanciación
                () -> assertThrows(Recipe.InvalidRecipeMetricException.class,
                        () -> new Recipe(new Recipe.AuthorId(1), 1, 1, "Sopa", "Desc", 20, 4).setPreparationTimeMinutes(-5)),
                () -> assertThrows(Recipe.InvalidRecipeMetricException.class,
                        () -> new Recipe(new Recipe.AuthorId(1), 1, 1, "Sopa", "Desc", 20, 4).setServings(0))
        );
    }

    @Test
    @DisplayName("Debe gestionar la adición de ingredientes correctamente")
    void shouldManageIngredientsCorrectly() {
        // Arrange
        Recipe recipe = new Recipe(new Recipe.AuthorId(1), 1, 1, "Pasta", "Desc", 15, 2);
        RecipeIngredient ingredient = new RecipeIngredient(1, 1, new BigDecimal("200"), "Macarrones", "Gramos");

        // Act
        recipe.addIngredient(ingredient);

        // Assert
        assertEquals(1, recipe.getIngredients().size());
        assertEquals("Macarrones", recipe.getIngredients().get(0).getIngredientName());
    }

    @Test
    @DisplayName("Debe garantizar que la lista de ingredientes expuesta sea inmutable")
    void shouldMaintainImmutabilityOfIngredientsList() {
        // Arrange
        Recipe recipe = new Recipe(new Recipe.AuthorId(1), 1, 1, "Ensalada", "Desc", 5, 1);
        List<RecipeIngredient> exposedIngredients = recipe.getIngredients();

        // Act & Assert
        // Verifica que el getter retorne un wrapper de solo lectura, forzando el paso por el Aggregate Root.
        assertThrows(UnsupportedOperationException.class, () -> {
            exposedIngredients.add(new RecipeIngredient(2, 2, BigDecimal.TEN, "Tomate", "Unidad"));
        });
    }

    @Test
    @DisplayName("Debe permitir el reemplazo atómico de la colección de ingredientes")
    void shouldAllowSettingIngredientsList() {
        // Arrange
        Recipe recipe = new Recipe(new Recipe.AuthorId(1), 1, 1, "Sopa", "Desc", 20, 4);
        List<RecipeIngredient> newIngredients = List.of(
                new RecipeIngredient(1, 1, BigDecimal.ONE, "Agua", "Litro"),
                new RecipeIngredient(2, 2, BigDecimal.valueOf(2), "Sal", "Pizca")
        );

        // Act
        recipe.setIngredients(newIngredients);

        // Assert
        assertEquals(2, recipe.getIngredients().size());
    }
}