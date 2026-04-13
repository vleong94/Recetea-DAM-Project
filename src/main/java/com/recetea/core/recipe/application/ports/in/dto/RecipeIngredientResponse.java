package com.recetea.core.recipe.application.ports.in.dto;

import java.math.BigDecimal;

/**
 * Define la estructura de datos para un ingrediente dentro del contexto de una receta específica.
 * Proporciona la información necesaria para mostrar la cantidad, la unidad de medida y el nombre
 * del ingrediente sin exponer la lógica interna de la entidad de dominio.
 *
 * @param ingredientId Identificador único del ingrediente en el catálogo.
 * @param unitId Identificador único de la unidad de medida asociada.
 * @param quantity Valor numérico de la cantidad del ingrediente.
 * @param ingredientName Nombre legible del ingrediente para el usuario.
 * @param unitName Nombre o símbolo de la unidad de medida (ej. gramos, litros).
 */
public record RecipeIngredientResponse(
        int ingredientId,
        int unitId,
        BigDecimal quantity,
        String ingredientName,
        String unitName
) {
    /**
     * El uso de BigDecimal asegura la precisión decimal requerida en las mediciones culinarias,
     * mientras que el formato record garantiza la inmutabilidad de los datos transferidos.
     */
}