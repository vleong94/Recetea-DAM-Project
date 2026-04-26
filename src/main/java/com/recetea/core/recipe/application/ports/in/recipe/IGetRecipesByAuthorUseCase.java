package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;
import com.recetea.core.user.domain.UserId;

public interface IGetRecipesByAuthorUseCase {

    PageResponse<RecipeSummaryResponse> execute(UserId authorId, PageRequest page);
}
