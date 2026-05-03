package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.social.application.ports.in.IIsFavoriteUseCase;
import com.recetea.core.social.application.ports.in.IToggleFavoriteUseCase;

/**
 * Favorite-management operations. Read ({@code isFavorite}) and write ({@code toggleFavorite})
 * are kept together because every UI flow that toggles also queries first to seed the button
 * state — splitting them would force every consumer to inject two parallel providers.
 */
public interface IFavoriteWriteProvider {
    IToggleFavoriteUseCase toggleFavorite();
    IIsFavoriteUseCase isFavorite();
}
