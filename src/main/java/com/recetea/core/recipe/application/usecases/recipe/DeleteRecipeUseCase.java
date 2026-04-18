package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.shared.application.ports.out.ITransactionManager;

public class DeleteRecipeUseCase implements IDeleteRecipeUseCase {
    private final IRecipeRepository recipeRepository;
    private final ITransactionManager transactionManager;

    public DeleteRecipeUseCase(IRecipeRepository recipeRepository, ITransactionManager transactionManager) {
        this.recipeRepository = recipeRepository;
        this.transactionManager = transactionManager;
    }

    @Override
    public void execute(int id) {
        transactionManager.execute(() -> recipeRepository.delete(id));
    }
}