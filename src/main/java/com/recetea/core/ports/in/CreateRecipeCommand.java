package com.recetea.core.ports.in;

import java.util.List;

/**
 * Data Transfer Object (Command): Transporta los datos crudos desde la UI
 * hacia el Use Case, evitando que la vista conozca las entidades del Domain.
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
    // Record anidado para la lista de la compra
    public record IngredientCommand(
            int ingredientId,
            int unitId,
            double quantity
    ) {}
}