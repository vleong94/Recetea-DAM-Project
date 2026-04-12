package com.recetea.core.usecases.recipe;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.out.IRecipeRepository;
import com.recetea.core.ports.in.dto.CreateRecipeCommand;
import com.recetea.core.ports.in.recipe.IUpdateRecipeUseCase;

import java.util.stream.Collectors;

/**
 * Application Layer: Caso de Uso para la actualización de recetas.
 * Reconcilia el estado enviado desde la UI con la persistencia transaccional.
 * Sincronizado para manejar la hidratación de nombres de ingredientes y unidades.
 */
public class UpdateRecipeUseCase implements IUpdateRecipeUseCase {
    private final IRecipeRepository repository;

    /**
     * Inyección del puerto de salida.
     */
    public UpdateRecipeUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(int recipeId, CreateRecipeCommand command) {
        // 1. Transformamos el Command en un objeto de Dominio (Aggregate Root)
        Recipe recipe = new Recipe(
                command.userId(),
                command.categoryId(),
                command.difficultyId(),
                command.title(),
                command.description(),
                command.preparationTimeMinutes(),
                command.servings()
        );

        // Asignamos el ID existente para que el repositorio sepa qué registro modificar
        recipe.setId(recipeId);

        // 2. Mapeo de sub-entidades (RecipeIngredient)
        // Actualizado para pasar los 5 parámetros requeridos por el nuevo constructor
        recipe.setIngredients(command.ingredients().stream()
                .map(i -> new RecipeIngredient(
                        i.ingredientId(),
                        i.unitId(),
                        i.quantity(),
                        i.ingredientName(), // <--- Mapeo de nombre corregido
                        i.unitName()        // <--- Mapeo de unidad corregido
                ))
                .collect(Collectors.toList()));

        // 3. Delegación al repositorio (Persistencia transaccional)
        repository.update(recipe);
    }
}