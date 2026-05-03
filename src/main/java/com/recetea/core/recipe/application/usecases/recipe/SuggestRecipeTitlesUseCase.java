package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.ISuggestRecipeTitlesUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;

import java.util.List;

/**
 * Title-prefix autocomplete for the dashboard's search field. Delegates to
 * {@code IRecipeRepository.suggestTitles}, which runs an
 * {@code ILIKE 'prefix%'} (anchored — no leading wildcard) so the
 * {@code title} index serves the lookup; a leading {@code %} would force a
 * sequential scan of {@code recipes} on every keystroke.
 *
 * <p>The repository implementation short-circuits null/blank prefixes and
 * non-positive limits to an empty list, so this layer doesn't need to
 * pre-validate.
 *
 * <p><b>ES — </b>Autocompletado por prefijo de título para el campo de
 * búsqueda del dashboard. Delega en
 * {@code IRecipeRepository.suggestTitles}, que ejecuta un
 * {@code ILIKE 'prefix%'} (anclado — sin comodín inicial) para que el
 * índice {@code title} sirva la consulta; un {@code %} inicial
 * forzaría un escaneo secuencial de {@code recipes} en cada
 * pulsación.
 *
 * <p>La implementación del repositorio corto-circuita los prefijos
 * null/en blanco y los límites no positivos a una lista vacía, así
 * que esta capa no necesita pre-validar.
 */
public class SuggestRecipeTitlesUseCase implements ISuggestRecipeTitlesUseCase {

    private final IRecipeRepository recipeRepository;

    public SuggestRecipeTitlesUseCase(IRecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<String> execute(String prefix, int limit) {
        return recipeRepository.suggestTitles(prefix, limit);
    }
}
