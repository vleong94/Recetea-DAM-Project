package com.recetea.core.usecases;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.IRecipeRepository;
import com.recetea.core.ports.in.CreateRecipeCommand;
import com.recetea.core.ports.in.IUpdateRecipeUseCase;

import java.util.stream.Collectors;

public class UpdateRecipeUseCase implements IUpdateRecipeUseCase {
    private final IRecipeRepository repository;

    public UpdateRecipeUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(int recipeId, CreateRecipeCommand command) {
        // Transformamos el Command en un objeto de Dominio
        Recipe recipe = new Recipe(
                command.userId(), command.categoryId(), command.difficultyId(),
                command.title(), command.description(), command.preparationTimeMinutes(), command.servings()
        );
        recipe.setId(recipeId);

        // Mapeo de sub-entidades (Mapping)
        recipe.setIngredients(command.ingredients().stream()
                .map(i -> new RecipeIngredient(i.ingredientId(), i.unitId(), i.quantity()))
                .collect(Collectors.toList()));

        repository.update(recipe);
    }
}