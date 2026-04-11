package com.recetea.core.usecases;

import com.recetea.core.ports.IRecipeRepository;
import com.recetea.core.ports.in.IDeleteRecipeUseCase;

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