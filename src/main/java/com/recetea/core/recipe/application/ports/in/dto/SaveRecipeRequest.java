package com.recetea.core.recipe.application.ports.in.dto;

import java.math.BigDecimal;
import java.util.List;

public record SaveRecipeRequest(
        int categoryId,
        int difficultyId,
        String title,
        String description,
        int preparationTimeMinutes,
        int servings,
        List<IngredientRequest> ingredients,
        List<StepRequest> steps
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

    /**
     * Define el subcontrato de datos para cada bloque operativo de preparación.
     * Asegura la transmisión íntegra del orden lógico y el contenido de la acción.
     *
     * @param stepOrder Entero que define la posición inmutable de la instrucción en el flujo.
     * @param instruction Cadena de texto con el detalle descriptivo de la acción a ejecutar.
     */
    public record StepRequest(
            int stepOrder,
            String instruction
    ) {}
}