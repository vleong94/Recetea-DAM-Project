package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;

public interface IGetAllRecipesUseCase {
    PageResponse<RecipeSummaryResponse> execute(PageRequest pageRequest);
}
