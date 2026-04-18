package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;
import com.recetea.core.recipe.application.ports.in.recipe.ISearchRecipesUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;

import java.util.List;

public class SearchRecipesUseCase implements ISearchRecipesUseCase {

    private final IRecipeRepository recipeRepository;

    public SearchRecipesUseCase(IRecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<RecipeSummaryResponse> execute(SearchCriteria criteria) {
        return recipeRepository.searchSummaries(criteria);
    }
}
