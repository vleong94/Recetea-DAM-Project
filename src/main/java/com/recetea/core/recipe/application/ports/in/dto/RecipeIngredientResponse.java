package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.UnitId;

import java.math.BigDecimal;

/**
 * One ingredient row hydrated for the recipe detail / edit views. Carries
 * both the ID-typed handles ({@code ingredientId} / {@code unitId}) and the
 * denormalised display strings ({@code ingredientName} / {@code unitName})
 * so the page renders without a second catalogue call. Names come from a
 * JOIN inside the LATERAL-JSONB hydration query — the use case never
 * resolves them in Java.
 *
 * <p><b>ES — </b>Una fila de ingrediente hidratada para las vistas de
 * detalle / edición de receta. Lleva tanto los manejadores tipados por id
 * ({@code ingredientId} / {@code unitId}) como las cadenas de presentación
 * desnormalizadas ({@code ingredientName} / {@code unitName}) para que la
 * página renderice sin una segunda llamada al catálogo. Los nombres
 * vienen de un JOIN dentro de la consulta de hidratación LATERAL-JSONB —
 * el caso de uso nunca los resuelve en Java.
 */
public record RecipeIngredientResponse(
        IngredientId ingredientId,
        UnitId unitId,
        BigDecimal quantity,
        String ingredientName,
        String unitName
) {}
