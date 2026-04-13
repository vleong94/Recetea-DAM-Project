package com.recetea.core.recipe.application.ports.in.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa el payload estructurado para la creación o mutación de una receta.
 * Actúa como un Inbound DTO (Data Transfer Object) inmutable que encapsula
 * todo el estado necesario para que el Use Case orqueste la transacción,
 * aislando el Domain de los frameworks de la capa de presentación.
 *
 * @param userId Identificador del usuario autor del registro.
 * @param categoryId Identificador de la clasificación taxonómica.
 * @param difficultyId Identificador del nivel de complejidad.
 * @param title Cadena de texto con el nombre principal de la receta.
 * @param description Cadena de texto con las instrucciones de preparación.
 * @param preparationTimeMinutes Entero que define la duración en minutos.
 * @param servings Entero que define el rendimiento en porciones.
 * @param ingredients Colección anidada con la composición de la receta.
 */
public record SaveRecipeRequest(
        int userId,
        int categoryId,
        int difficultyId,
        String title,
        String description,
        int preparationTimeMinutes,
        int servings,
        List<IngredientRequest> ingredients
) {
    /**
     * Define el subcontrato de datos para cada elemento de la composición.
     * Transporta tanto los Foreign Keys (IDs) requeridos por el Data Store,
     * como los descriptores nominales necesarios para instanciar de forma
     * segura el Value Object en el Domain Model sin requerir hidratación adicional.
     *
     * @param ingredientId Identificador único del ingrediente en el catálogo.
     * @param unitId Identificador único de la unidad de medida.
     * @param quantity Magnitud escalar de tipo BigDecimal para alta precisión en cálculo.
     * @param ingredientName Representación textual nominal del ingrediente.
     * @param unitName Representación textual nominal de la unidad de medida.
     */
    public record IngredientRequest(
            int ingredientId,
            int unitId,
            BigDecimal quantity,
            String ingredientName,
            String unitName
    ) {}
}