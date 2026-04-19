package com.recetea.core.social.application.ports.in;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;

import java.util.List;

public interface IGetUserFavoritesUseCase {
    List<RecipeSummaryResponse> execute();
}
