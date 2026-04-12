package com.recetea.core.usecases.recipe;

import com.recetea.core.ports.out.IRecipeRepository;
import com.recetea.core.ports.in.recipe.IDeleteRecipeUseCase;

public class DeleteRecipeUseCase implements IDeleteRecipeUseCase {

    private final IRecipeRepository repository;

    public DeleteRecipeUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(int recipeId) {
        repository.delete(recipeId);
    }
}