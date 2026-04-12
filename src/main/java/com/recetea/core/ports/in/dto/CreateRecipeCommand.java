package com.recetea.core.ports.in.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object (Command): Transporta los datos crudos desde la UI
 * hacia el Use Case, evitando que la vista conozca las entidades del Domain.
 * Refactorizado para incluir nombres descriptivos para la visualización inmediata.
 */
public record CreateRecipeCommand(
        int userId,
        int categoryId,
        int difficultyId,
        String title,
        String description,
        int preparationTimeMinutes,
        int servings,
        List<IngredientCommand> ingredients
) {
    /**
     * Record anidado para la lista de ingredientes.
     * Incluye los nombres para que la UI pueda renderizar la tabla sin consultas extra.
     */
    public record IngredientCommand(
            int ingredientId,
            int unitId,
            BigDecimal quantity, // <--- Coma corregida aquí
            String ingredientName,
            String unitName
    ) {}
}