package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetAllRecipesUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;

import java.util.List;

public class GetAllRecipesUseCase implements IGetAllRecipesUseCase {

    private final IRecipeRepository repository;

    public GetAllRecipesUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RecipeSummaryResponse> execute() {
        return repository.findAll().stream()
                .map(recipe -> new RecipeSummaryResponse(
                        recipe.getId().value(),
                        recipe.getTitle(),
                        recipe.getCategory().getName(),
                        recipe.getDifficulty().getName(),
                        recipe.getPreparationTimeMinutes().value(),
                        recipe.getServings().value()
                ))
                .toList();
    }
}
