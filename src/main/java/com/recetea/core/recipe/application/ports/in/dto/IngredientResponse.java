package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.IngredientId;

/**
 * Catalogue projection used by ingredient pickers (dashboard search drawer,
 * recipe form ingredient row). Two fields — id + display name — kept
 * intentionally narrow because the catalogue lookup hits {@code findAll} and
 * gets cached for the JVM lifetime; bloating the row would raise the cache
 * footprint without buying anything for the picker UI.
 *
 * <p><b>ES — </b>Proyección de catálogo usada por los selectores de
 * ingrediente (cajón de búsqueda del dashboard, fila de ingredientes del
 * formulario de receta). Dos campos — id + nombre de presentación —
 * mantenidos deliberadamente estrechos porque la consulta de catálogo
 * dispara {@code findAll} y se cachea durante toda la vida del JVM;
 * inflar la fila aumentaría la huella de la caché sin aportar nada a la
 * UI del selector.
 */
public record IngredientResponse(
        IngredientId id,
        String name
) {}
