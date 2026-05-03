package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.UnitId;

/**
 * Catalogue projection for unit-of-measure pickers in the recipe form. The
 * abbreviation ({@code Unit.abbreviation()}) is intentionally not exposed
 * here — {@code IngredientTableComponent} renders the full {@code name}
 * directly; the abbreviation is only loaded when an existing recipe is
 * hydrated for read.
 *
 * <p><b>ES — </b>Proyección de catálogo para los selectores de unidades
 * de medida en el formulario de receta. La abreviatura
 * ({@code Unit.abbreviation()}) no se expone aquí intencionadamente —
 * {@code IngredientTableComponent} renderiza el {@code name} completo
 * directamente; la abreviatura sólo se carga cuando se hidrata una
 * receta existente para lectura.
 */
public record UnitResponse(
        UnitId id,
        String name
) {}
