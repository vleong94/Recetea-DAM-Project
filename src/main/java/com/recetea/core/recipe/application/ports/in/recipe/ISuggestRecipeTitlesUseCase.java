package com.recetea.core.recipe.application.ports.in.recipe;

import java.util.List;

/**
 * Lightweight prefix lookup for the dashboard autocomplete dropdown.
 *
 * <p>Backed by {@code RecipeSqlRegistry.SUGGEST_TITLES} — an anchored ILIKE
 * with no leading wildcard, so the existing {@code recipes_title} index
 * serves the lookup in O(log n). Caller is expected to debounce; a cap of
 * 10 suggestions is typical at the call site.
 *
 * <p><b>ES — </b>Búsqueda ligera por prefijo para el desplegable de
 * autocompletado del dashboard.
 *
 * <p>Respaldada por {@code RecipeSqlRegistry.SUGGEST_TITLES} — un ILIKE
 * anclado sin comodín inicial, de modo que el índice
 * {@code recipes_title} existente sirve la consulta en O(log n). Se
 * espera que el llamador aplique debounce; un tope de 10 sugerencias es
 * lo habitual en el sitio de llamada.
 */
public interface ISuggestRecipeTitlesUseCase {
    List<String> execute(String prefix, int limit);
}
