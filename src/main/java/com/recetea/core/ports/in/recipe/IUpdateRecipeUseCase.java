package com.recetea.core.ports.in.recipe;

import com.recetea.core.ports.in.dto.CreateRecipeCommand;

// Usaremos el mismo Command que en la creación, ya que los datos necesarios son idénticos
public interface IUpdateRecipeUseCase {
    void execute(int recipeId, CreateRecipeCommand command);
}