package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.user.domain.UserId;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lightweight projection used by every list-style view (dashboard gallery,
 * profile favorites tab, search results). Eleven fields — flat, no nested
 * objects — chosen so a single SELECT against the {@code SELECT_SUMMARIES}
 * template populates the row in one round-trip without touching child tables.
 *
 * <p><b>Why social metrics are present.</b> {@code averageScore} and
 * {@code totalRatings} are denormalised on {@code recipes} and updated
 * synchronously by the rating-write path, so list queries never need to
 * JOIN the {@code ratings} table. {@link RecipeSummaryRowSupport} owns the
 * SQL template — every projection (find-all, find-by-ids, find-by-author,
 * search) routes through it so column-name drift cannot diverge.
 *
 * <p>{@code mainMediaStorageKey} is the {@code storage_key} of the row
 * with {@code is_main = true}; resolved in the UI through
 * {@code NavigationService.resolveMediaUri(...)} which knows the configured
 * storage base path. Null when the recipe has no media.
 *
 * <p>{@code ingredientIds} is defensively copied via {@code List.copyOf} in
 * the compact constructor so the immutable contract holds even when the
 * caller hands a mutable list.
 *
 * <p><b>ES — </b>Proyección ligera usada por todas las vistas tipo lista
 * (galería del dashboard, pestaña de favoritos en el perfil, resultados
 * de búsqueda). Once campos — planos, sin objetos anidados — elegidos
 * para que un único SELECT contra la plantilla {@code SELECT_SUMMARIES}
 * rellene la fila en un solo round-trip sin tocar las tablas hijas.
 *
 * <p><b>Por qué hay métricas sociales aquí.</b> {@code averageScore} y
 * {@code totalRatings} están desnormalizadas en {@code recipes} y se
 * actualizan de forma síncrona por la ruta de escritura de valoraciones,
 * así que las consultas de listado nunca necesitan hacer JOIN con la
 * tabla {@code ratings}. {@link RecipeSummaryRowSupport} es dueña de la
 * plantilla SQL — toda proyección (find-all, find-by-ids, find-by-author,
 * search) pasa por ella para que la deriva de nombres de columna no pueda
 * divergir.
 *
 * <p>{@code mainMediaStorageKey} es la {@code storage_key} de la fila con
 * {@code is_main = true}; se resuelve en la UI a través de
 * {@code NavigationService.resolveMediaUri(...)}, que conoce la ruta base
 * de storage configurada. Nulo cuando la receta no tiene multimedia.
 *
 * <p>{@code ingredientIds} se copia defensivamente con {@code List.copyOf}
 * en el constructor compacto, de modo que el contrato de inmutabilidad
 * se mantenga aunque el llamador entregue una lista mutable.
 */
public record RecipeSummaryResponse(
        RecipeId id,
        String title,
        String categoryName,
        String difficultyName,
        int prepTimeMinutes,
        int servings,
        BigDecimal averageScore,
        int totalRatings,
        String mainMediaStorageKey,
        UserId authorId,
        String authorUsername,
        List<IngredientId> ingredientIds
) {
    public RecipeSummaryResponse {
        ingredientIds = ingredientIds == null ? List.of() : List.copyOf(ingredientIds);
    }
}
