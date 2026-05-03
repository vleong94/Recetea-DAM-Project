package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;

import java.util.Optional;

/**
 * Loads a recipe's full detail view: metadata + ingredients + steps + ratings
 * + media, plus the per-viewer {@code alreadyRatedByCurrentUser} flag that
 * drives the rating widget's enabled state.
 *
 * <p>The implementation routes through the LATERAL-JSONB single-round-trip
 * query in {@code RecipeSqlRegistry.SELECT_FULL_AGGREGATE} — one trip for the
 * recipe + four child collections together, hydrated into the immutable
 * {@code Recipe} record in one constructor call. {@code JsonbPerformanceTest}
 * asserts this path stays under 100 ms even at 50 ingredients + 50 steps +
 * 1 000 ratings.
 *
 * <p>Returns {@link Optional#empty()} when the id has no matching row —
 * <em>not</em> a thrown exception. Lookup-and-decide flows (favouriting an
 * id that may have been deleted, navigating from a stale link) handle the
 * empty branch directly without a try/catch.
 *
 * <p><b>ES — </b>Carga la vista de detalle completa de una receta:
 * metadatos + ingredientes + pasos + valoraciones + multimedia, más el
 * flag por-espectador {@code alreadyRatedByCurrentUser} que controla el
 * estado habilitado del widget de valoración.
 *
 * <p>La implementación se enruta a través de la consulta LATERAL-JSONB
 * de un solo round-trip en {@code RecipeSqlRegistry.SELECT_FULL_AGGREGATE}
 * — un único viaje para la receta + cuatro colecciones hijas juntas,
 * hidratadas en el record inmutable {@code Recipe} en una sola llamada
 * al constructor. {@code JsonbPerformanceTest} garantiza que esta ruta
 * se mantiene por debajo de 100 ms incluso con 50 ingredientes + 50
 * pasos + 1 000 valoraciones.
 *
 * <p>Devuelve {@link Optional#empty()} cuando el id no tiene fila
 * correspondiente — <em>no</em> una excepción. Los flujos de buscar-y-
 * decidir (marcar como favorito un id que pudo borrarse, navegar desde
 * un enlace obsoleto) manejan la rama vacía directamente sin try/catch.
 */
public interface IGetRecipeByIdUseCase {

    Optional<RecipeDetailResponse> execute(RecipeId recipeId);
}
