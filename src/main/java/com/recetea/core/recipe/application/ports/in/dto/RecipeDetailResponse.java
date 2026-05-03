package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;
import com.recetea.core.user.domain.UserId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full hydration of a recipe for the detail view — the only flow where every
 * child collection (ingredients, steps, media, ratings) must be present at
 * once. Backed by the LATERAL-JSONB query in {@code RecipeSqlRegistry}: a
 * single round-trip projects the parent row plus four JSON-aggregated child
 * arrays which the row mapper splits into the lists below.
 *
 * <p><b>{@code alreadyRatedByCurrentUser}</b> is computed by
 * {@code GetRecipeByIdUseCase.mapToResponse(...)} from the current session
 * (anonymous → false). The detail controller uses it to disable
 * {@code RatingComponent} so the user cannot vote twice; the vote-uniqueness
 * constraint at the DB layer is the authoritative guarantee, this flag is
 * UX only.
 *
 * <p><b>Display denormalisation.</b> The category / difficulty / author
 * names are denormalised onto the response so the detail template never
 * needs catalogue lookups — both the IDs and the names ride along.
 *
 * <p><b>ES — </b>Hidratación completa de una receta para la vista de
 * detalle — el único flujo donde todas las colecciones hijas
 * (ingredientes, pasos, multimedia, valoraciones) deben estar presentes
 * a la vez. Respaldada por la consulta LATERAL-JSONB en
 * {@code RecipeSqlRegistry}: un único round-trip proyecta la fila padre
 * más cuatro arrays hijos agregados como JSON que el row mapper separa
 * en las listas siguientes.
 *
 * <p><b>{@code alreadyRatedByCurrentUser}</b> lo calcula
 * {@code GetRecipeByIdUseCase.mapToResponse(...)} a partir de la sesión
 * actual (anónimo → false). El controlador de detalle lo usa para
 * deshabilitar {@code RatingComponent} y que el usuario no pueda votar
 * dos veces; la restricción de unicidad de voto en la BD es la garantía
 * autoritativa, este flag es sólo UX.
 *
 * <p><b>Desnormalización de presentación.</b> Los nombres de
 * categoría / dificultad / autor se desnormalizan en la respuesta para
 * que la plantilla de detalle nunca necesite consultas al catálogo —
 * tanto los IDs como los nombres viajan juntos.
 */
public record RecipeDetailResponse(
        RecipeId id,
        UserId userId,
        String authorUsername,
        CategoryId categoryId,
        String categoryName,
        DifficultyId difficultyId,
        String difficultyName,
        String title,
        String description,
        int prepTimeMinutes,
        int servings,
        List<RecipeIngredientResponse> ingredients,
        List<RecipeStepResponse> steps,
        BigDecimal averageScore,
        int totalRatings,
        List<RecipeMediaResponse> media,
        boolean alreadyRatedByCurrentUser,
        List<RatingDetail> ratings
) {
    /**
     * A single step in render order. {@code stepOrder} is 1-based and unique within a recipe.
     *
     * <p><b>ES — </b>Un único paso en orden de renderizado. {@code stepOrder}
     * es 1-based y único dentro de una receta.
     */
    public record RecipeStepResponse(
            int stepOrder,
            String instruction
    ) {}

    /**
     * Media row exposed to the UI. {@code storageKey} is the opaque handle
     * (flat filename for local FS, object path for Supabase) — the UI must
     * pass it through {@code NavigationService.resolveMediaUri(...)} to
     * obtain a viewable URI. {@code isMain} marks the cover image; only one
     * row per recipe carries it (partial unique index in the schema).
     *
     * <p><b>ES — </b>Fila de multimedia expuesta a la UI. {@code storageKey}
     * es el manejador opaco (nombre de archivo plano para FS local, ruta
     * de objeto para Supabase) — la UI debe pasarlo por
     * {@code NavigationService.resolveMediaUri(...)} para obtener una URI
     * visualizable. {@code isMain} marca la imagen de portada; sólo una
     * fila por receta lo lleva (índice único parcial en el esquema).
     */
    public record RecipeMediaResponse(
            RecipeMediaId id,
            String storageKey,
            String storageProvider,
            String mimeType,
            long sizeBytes,
            boolean isMain,
            int sortOrder
    ) {}

    /**
     * Projection of a single rating for social display. username may be null if the voter
     * account has been deleted since the rating was cast.
     *
     * <p><b>ES — </b>Proyección de una única valoración para la
     * presentación social. El username puede ser null si la cuenta del
     * votante ha sido eliminada después de emitir la valoración.
     */
    public record RatingDetail(
            String username,
            int score,
            String comment,
            LocalDateTime date
    ) {}
}
