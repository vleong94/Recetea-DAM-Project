package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.IngredientId;

import java.util.List;

/**
 * Filter bag for {@code ISearchRecipesUseCase}. Every field is nullable —
 * a null value means "no constraint on this dimension" — so the same
 * record powers everything from a single-criterion lookup to the
 * dashboard's full filter drawer.
 *
 * <p>{@code categoryName} / {@code difficultyName} / {@code authorUsername}
 * carry strings rather than ID-typed VOs because the dashboard's combos
 * bind to the display value; the SQL side joins on the catalogue tables
 * to resolve them. {@code ingredientIds} is the multi-ingredient filter
 * — translated to an {@code AND}-match in the search gateway via
 * {@code HAVING COUNT(DISTINCT)} so a recipe must contain every requested
 * ingredient (not just any one of them).
 *
 * <p>Adding a new filter requires an additive change here AND in
 * {@code RecipeSearchGateway.buildWhereClause} AND in
 * {@code RecipeSearchViewModel.predicateFor} — the three are kept in sync
 * manually (see {@code CLAUDE.md}, "Known Gaps").
 *
 * <p><b>ES — </b>Bolsa de filtros para {@code ISearchRecipesUseCase}.
 * Todos los campos son anulables — un valor null significa "sin
 * restricción en esta dimensión" — para que el mismo record sirva tanto
 * para una búsqueda por un criterio único como para el cajón de filtros
 * completo del dashboard.
 *
 * <p>{@code categoryName} / {@code difficultyName} /
 * {@code authorUsername} llevan cadenas en lugar de VOs tipados por id
 * porque los combos del dashboard se enlazan al valor de presentación;
 * el lado SQL hace JOIN con las tablas de catálogo para resolverlos.
 * {@code ingredientIds} es el filtro multi-ingrediente — traducido a una
 * coincidencia AND en el search gateway mediante
 * {@code HAVING COUNT(DISTINCT)}, de modo que una receta deba contener
 * todos los ingredientes solicitados (no sólo uno cualquiera).
 *
 * <p>Añadir un nuevo filtro requiere un cambio aditivo aquí Y en
 * {@code RecipeSearchGateway.buildWhereClause} Y en
 * {@code RecipeSearchViewModel.predicateFor} — los tres se mantienen
 * sincronizados manualmente (véase {@code CLAUDE.md}, "Known Gaps").
 */
public record SearchCriteria(
        String title,
        Integer maxPreparationTime,
        Integer minServings,
        String categoryName,
        String difficultyName,
        List<IngredientId> ingredientIds,
        String authorUsername,
        Integer minScore
) {
    public SearchCriteria {
        ingredientIds = ingredientIds == null ? List.of() : List.copyOf(ingredientIds);
    }
}
