package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;

import java.util.List;

public interface ISearchRecipesUseCase {
    List<RecipeSummaryResponse> execute(SearchCriteria criteria);
}
