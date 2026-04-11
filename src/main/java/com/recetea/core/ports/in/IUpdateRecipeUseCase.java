package com.recetea.core.ports.in;

// Usaremos el mismo Command que en la creación, ya que los datos necesarios son idénticos
public interface IUpdateRecipeUseCase {
    void execute(int recipeId, CreateRecipeCommand command);
}