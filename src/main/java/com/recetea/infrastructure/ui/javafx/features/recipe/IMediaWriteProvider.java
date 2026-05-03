package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.media.IAttachMediaUseCase;

/** Media attachment write operations. Granular interface segregated out of {@link RecipeCommandProvider}. */
public interface IMediaWriteProvider {
    IAttachMediaUseCase attachMedia();
}
