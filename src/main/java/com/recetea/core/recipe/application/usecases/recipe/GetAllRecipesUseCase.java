package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetAllRecipesUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;

public class GetAllRecipesUseCase implements IGetAllRecipesUseCase {

    private final IRecipeRepository repository;

    public GetAllRecipesUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResponse<RecipeSummaryResponse> execute(PageRequest pageRequest) {
        return repository.findAllSummaries(pageRequest);
    }
}
