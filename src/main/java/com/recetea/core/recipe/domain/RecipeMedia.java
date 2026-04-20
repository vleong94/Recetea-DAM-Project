package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;

import java.util.Objects;

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
        Objects.requireNonNull(recipeId, "recipeId no puede ser nulo.");
        Objects.requireNonNull(storageKey, "storageKey no puede ser nulo.");
        if (storageKey.isBlank()) throw new IllegalArgumentException("storageKey no puede estar vacío.");
        Objects.requireNonNull(storageProvider, "storageProvider no puede ser nulo.");
        if (storageProvider.isBlank()) throw new IllegalArgumentException("storageProvider no puede estar vacío.");
        Objects.requireNonNull(mimeType, "mimeType no puede ser nulo.");
        if (mimeType.isBlank()) throw new IllegalArgumentException("mimeType no puede estar vacío.");
        if (sizeBytes < 0) throw new IllegalArgumentException("sizeBytes debe ser >= 0.");
        if (sortOrder < 0) throw new IllegalArgumentException("sortOrder debe ser >= 0.");
    }
}
