package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;

import java.util.Objects;

/**
 * One media item attached to a recipe — image or future video. Eight
 * components, fully immutable, validated fail-fast in the compact
 * constructor.
 *
 * <p><b>{@code id} may be null</b> — it represents the create-path before
 * persistence assigns a value. Once persisted, the id is stable and
 * passed back to {@code IRemoveMediaUseCase} / {@code Recipe.setMainMedia}.
 *
 * <p>{@code storageProvider} ({@code "LOCAL"} / {@code "SUPABASE"})
 * describes where {@code storageKey} resolves — the URI builder in
 * {@code NavigationService.resolveMediaUri(...)} routes on this field.
 *
 * <p>{@code isMain} marks the cover image; only one row per recipe carries
 * it, enforced by the partial unique index on the schema. Promotion is
 * handled by {@code Recipe.addMedia} / {@code Recipe.setMainMedia} — never
 * by direct row writes.
 *
 * <p><b>ES — </b>Un elemento multimedia asociado a una receta — imagen o,
 * en el futuro, vídeo. Ocho componentes, totalmente inmutable, validado
 * fail-fast en el constructor compacto.
 *
 * <p><b>{@code id} puede ser nulo</b> — representa el camino de creación
 * antes de que la persistencia le asigne un valor. Una vez persistido, el
 * id es estable y se pasa de vuelta a {@code IRemoveMediaUseCase} /
 * {@code Recipe.setMainMedia}.
 *
 * <p>{@code storageProvider} ({@code "LOCAL"} / {@code "SUPABASE"})
 * describe dónde se resuelve {@code storageKey} — el constructor de URIs
 * en {@code NavigationService.resolveMediaUri(...)} enruta en función de
 * este campo.
 *
 * <p>{@code isMain} marca la imagen de portada; solo una fila por receta
 * lo lleva, garantizado por el índice único parcial del esquema. La
 * promoción la gestionan {@code Recipe.addMedia} /
 * {@code Recipe.setMainMedia} — nunca con escrituras directas a la fila.
 */
public record RecipeMedia(
        RecipeMediaId id,
        RecipeId recipeId,
        String storageKey,
        String storageProvider,
        String mimeType,
        long sizeBytes,
        boolean isMain,
        int sortOrder) {

    public RecipeMedia {
        Objects.requireNonNull(recipeId, "recipeId must not be null.");
        Objects.requireNonNull(storageKey, "storageKey must not be null.");
        if (storageKey.isBlank()) throw new IllegalArgumentException("storageKey must not be blank.");
        Objects.requireNonNull(storageProvider, "storageProvider must not be null.");
        if (storageProvider.isBlank()) throw new IllegalArgumentException("storageProvider must not be blank.");
        Objects.requireNonNull(mimeType, "mimeType must not be null.");
        if (mimeType.isBlank()) throw new IllegalArgumentException("mimeType must not be blank.");
        if (sizeBytes < 0) throw new IllegalArgumentException("sizeBytes must be >= 0.");
        if (sortOrder < 0) throw new IllegalArgumentException("sortOrder must be >= 0.");
    }

    /**
     * Returns a structurally identical copy with {@code isMain} flipped to the requested value.
     * The classic record "wither": preferred over an external clone helper because the record
     * already knows its own component layout.
     */
    public RecipeMedia withIsMain(boolean isMain) {
        return new RecipeMedia(id, recipeId, storageKey, storageProvider, mimeType, sizeBytes, isMain, sortOrder);
    }
}
