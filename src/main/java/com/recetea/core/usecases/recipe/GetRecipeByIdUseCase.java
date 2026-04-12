package com.recetea.core.usecases.recipe;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.out.IRecipeRepository;
import com.recetea.core.ports.in.recipe.IGetRecipeByIdUseCase;

import java.util.Optional;

public class GetRecipeByIdUseCase implements IGetRecipeByIdUseCase {

    private final IRecipeRepository repository;

    public GetRecipeByIdUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Recipe> execute(int recipeId) {
        return repository.findById(recipeId);
    }
}