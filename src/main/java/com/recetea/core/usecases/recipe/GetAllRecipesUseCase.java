package com.recetea.core.usecases.recipe;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.out.IRecipeRepository;
import com.recetea.core.ports.in.recipe.IGetAllRecipesUseCase;

import java.util.List;

/**
 * Application Layer: Orquesta la recuperación de datos.
 */
public class GetAllRecipesUseCase implements IGetAllRecipesUseCase {

    private final IRecipeRepository repository;

    // Dependency Injection
    public GetAllRecipesUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Recipe> execute() {
        return repository.findAll();
    }
}